---
title: "MCP 보안: OAuth2와 JWT로 지키는 MCP 클라이언트와 서버"
description: "호출 주체에 따라 달라지는 MCP 보안 환경을 나누고, mcp-security로 OAuth2 클라이언트, JWT 리소스 서버, 인가 서버를 구성하는 방법을 정리합니다."
tags:
  - 5장
---

# MCP 보안: OAuth2와 JWT로 지키는 MCP 클라이언트와 서버

<div class="post-meta" markdown>
<span class="tier-chip tx">횡단 관심사</span> 책 5.4절, 실습 5.5절 | 예제 [`chapter5`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter5)
</div>

앞의 두 글에서 [MCP 클라이언트](11-mcp-basics-and-spring-ai-mcp-client.md)로 원격 서버에 연결하고 [MCP 서버](12-building-an-mcp-server-with-spring-ai.md)를 직접 만들었습니다. 서버를 네트워크에 내놓으면 따져야 할 것이 생깁니다. 요청을 보낸 쪽이 누구인지, 그 주체에게 이 툴을 실행할 권한이 있는지입니다.

결제 승인이나 사내 데이터 조회 같은 툴을 인증 없이 열어 두면 곧바로 보안 사고로 이어집니다. 게다가 어떤 툴을 부를지는 사람이 버튼으로 고르지 않고 자연어 요청을 받은 모델이 판단합니다. 이 글은 호출 주체에 따라 MCP 보안 환경을 나눈 뒤, 커뮤니티 프로젝트 mcp-security로 클라이언트와 서버, 인가 서버를 구성하는 방법을 정리합니다.

## MCP 보안 환경

MCP 인가 명세는 HTTP 기반 전송에 OAuth 방식의 인가 절차를 권장합니다. 인가가 필요한데 아직 증명되지 않은 요청에는 서버가 401 Unauthorized로 응답하고, 클라이언트는 이 응답을 계기로 인가를 시작합니다. STDIO 전송은 이 절차 대신 실행 환경에서 자격 증명을 얻습니다.

전송 방식에 호출 주체까지 함께 보면 MCP 보안 환경은 네 가지로 나눌 수 있습니다.

| 환경 | 예 | 호출 주체 | 보안의 초점 |
| --- | --- | --- | --- |
| 로컬 앱 → 로컬 MCP 서버(STDIO) | 클로드 코드가 내 PC의 파일 시스템 서버를 실행 | 단일 사용자 | OAuth 대상이 아님. 어떤 서버 바이너리를 믿고 실행할지 |
| 로컬 앱 → 원격 MCP 서버 | 클로드 코드가 깃허브 MCP 서버에 연결 | 단일 사용자 | 원격 서버가 사용자를 인증하는 OAuth |
| 자동화 시스템 → 원격 MCP 서버 | 야간 배치, CI/CD, 장기 실행 에이전트 | 시스템 | 로그인 사용자가 없는 M2M 호출 |
| 엔터프라이즈 서비스 → 원격 MCP 서버 | 사내 AI 챗봇이 결재 시스템 서버를 호출 | 여러 사용자를 대행 | 누구를 대신하는지, 사용자별 인가 |

특히 까다로운 것은 마지막 환경입니다. 모든 요청을 시스템 계정 하나로 보내면 원격 MCP 서버는 실제 사용자를 모른 채 넓은 시스템 권한으로 요청을 처리하게 됩니다. 프롬프트 인젝션 때문에 모델이 사용자 의도와 달리 권한 밖의 툴을 부르려 할 수도 있습니다. 그래서 제로 트러스트 관점에서 클라이언트는 모델을, 서버는 클라이언트를 믿지 않습니다. 사용자 권한을 원격 MCP 서버까지 전달하고, 서버가 요청마다 토큰의 권한을 세밀하게 검증하는 구조가 더 안전합니다.

그렇다고 시스템 권한이 필요 없어지지는 않습니다. 애플리케이션이 뜨면서 서버에 연결하고 툴 목록을 가져올 때는 로그인한 사용자가 아직 없습니다. 시스템이 책임지는 초기화는 시스템 권한으로, 사용자가 요청한 툴 호출은 그 사용자의 권한으로 나누는 것이 엔터프라이즈 MCP 보안의 중심입니다.

## 스프링 AI의 MCP 시큐리티 구조

스프링이 보안을 스프링 시큐리티라는 별도 계층에 맡겨 온 것처럼, 스프링 AI도 MCP 보안을 코어에 넣지 않았습니다. 공식 레퍼런스가 소개하는 커뮤니티 프로젝트 mcp-security가 스프링 시큐리티의 OAuth2 모델을 MCP 통신에 연결하므로, MCP 전용 보안 체계를 새로 배울 필요가 없습니다. 모듈은 OAuth2의 역할에 맞춰 셋으로 나뉩니다.

| 모듈 | 역할 | 바탕이 되는 스프링 시큐리티 기능 |
| --- | --- | --- |
| `mcp-client-security` | 원격 MCP 서버 호출에 알맞은 토큰을 요청마다 넣음 | OAuth2 클라이언트 |
| `mcp-server-security` | MCP 서버 앞에서 JWT나 API 키를 검증 | OAuth2 리소스 서버 |
| `mcp-authorization-server` | MCP 클라이언트에 토큰을 발급 | 스프링 인가 서버 |

세 모듈은 스프링 AI BOM이 관리하지 않으므로, 사용하는 스프링 AI에 맞는 버전(책 기준 `0.1.13`)을 직접 지정합니다. 토큰 발급은 스프링 인가 서버 대신 옥타, 키클록 같은 외부 인증 제공자(IdP)가 맡을 수도 있습니다.

<figure class="wide-figure" markdown>
![mcp-security의 JWT 기반 MCP 시큐리티 구조](../assets/figures/fig5-7.png)
<figcaption>mcp-security의 JWT 기반 MCP 시큐리티 구조</figcaption>
</figure>

MCP 서버는 기동할 때나 처음 토큰을 검증할 때 인가 서버에서 JWT 서명 검증용 공개키 집합(JWKS)을 받아 메모리에 캐싱합니다. 클라이언트는 인가 서버에서 받은 JWT 액세스 토큰을 Bearer 토큰으로 실어 보냅니다. 서버는 요청마다 인가 서버에 토큰을 물어보는 불투명 토큰 방식 대신, 캐싱한 공개키로 서명과 주요 클레임을 직접 검증한 뒤 허용된 요청만 처리합니다.

## 클라이언트 시큐리티 설정과 세부 구현

### 정적 헤더 대신 OAuth

설정 파일에 고정 토큰을 넣어 헤더로 보내는 방식은 단순합니다. 하지만 MCP 통신은 연결을 맺고 `initialize`와 `tools/list`를 거친 뒤 `tools/call`로 이어지는 흐름입니다. 고정 헤더로는 토큰 만료와 재발급을 다루기 어렵고, 사용자 요청의 인증 문맥을 MCP 호출까지 전달하기도, 시스템 권한과 사용자 권한을 나누기도 어렵습니다. 스프링 AI 커뮤니티가 연결 설정에 고정 헤더를 넣는 방향을 택하지 않은 이유입니다.

<figure class="wide-figure" markdown>
![정적 헤더 주입 방식과 OAuth 기반 MCP 클라이언트 시큐리티의 비교](../assets/figures/fig5-8.png)
<figcaption>정적 헤더 주입 방식과 OAuth 기반 MCP 클라이언트 시큐리티의 비교</figcaption>
</figure>

그림 아래쪽처럼 클라이언트 시큐리티는 스프링 시큐리티의 OAuth2 클라이언트로 토큰을 받아 요청마다 넣고, 만료되면 인가 서버에서 새로 받습니다. 고정 헤더는 로컬 테스트나 단순한 연결 확인에는 쓸 만하지만, 원격 서버와 계속 주고받는 운영 환경에서는 한계가 있습니다.

### 공통 설정

`mcp-client-security`는 원격 전송 전용이라 STDIO는 지원하지 않습니다. HttpClient 기반 `spring-ai-starter-mcp-client`와 WebClient 기반 `spring-ai-starter-mcp-client-webflux`에서 모두 쓸 수 있지만 클라이언트는 `McpSyncClient`만 지원합니다. 인가 흐름은 호출 주체로 고릅니다. 로그인한 사용자를 대신하면 인가 코드, 사용자 없는 시스템 간 호출이면 클라이언트 자격 증명, 기동 시점의 시스템 호출과 이후의 사용자 호출이 함께 있으면 하이브리드 흐름입니다.

5장 실습 서버는 로컬 학습용이라 인증 없이 열려 있지만, 실습 클라이언트 설정에는 보안 모듈과 맞물리는 두 값이 이미 들어 있습니다.

```yaml title="application.yml"
--8<-- "chapter5/src/main/resources/application.yml:10:24"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter5/src/main/resources/application.yml)</span>

`spring.ai.mcp.client.type`은 `SYNC`여야 합니다. `spring.ai.mcp.client.initialized`는 기본값이 `true`이며, 이 경우 애플리케이션이 뜨면서 `initialize`와 `tools/list`를 먼저 호출합니다. 아직 로그인한 사용자가 없을 때 나가는 호출이므로, 이 속성은 기동 시점의 MCP 호출을 어떤 권한으로 보낼지 정하는 보안 설정이기도 합니다. 인가 코드 흐름이라면 `false`로 끄는 편이 안전하고, 초기화를 시스템 권한으로 처리하는 클라이언트 자격 증명과 하이브리드 흐름은 `true`를 그대로 둡니다.

여기에 스프링 시큐리티의 `spring.security.oauth2.client` 설정으로 인가 서버 정보(`provider`)의 `issuer-uri`와 흐름별 클라이언트 등록 정보(`registration`)를 더합니다. 책의 예에서는 사용자 권한용 `authserver`가 `authorization_code`를, 시스템 권한용 `authserver-client-credentials`가 `client_credentials`를 씁니다. 스프링 시큐리티는 이 등록 ID로 `ClientRegistrationRepository`에서 설정을 찾으므로, 뒤에서 구현체를 만들 때도 이 ID를 넘깁니다. 코드에서는 `SecurityFilterChain`에 `oauth2Client()`를 켜고, 어느 흐름에나 필요한 `McpClientCustomizer`를 등록합니다.

```java title="McpClientSecurityConfiguration.java (책 예제 발췌)"
@Bean
McpClientCustomizer<McpClient.SyncSpec> syncClientCustomizer() {
  return (name, syncSpec) ->
      syncSpec.transportContextProvider(
          // 현재 스레드/요청의 인증을 추출해 MCP 전송 계층으로 넘김
          new AuthenticationMcpTransportContextProvider()
      );
}
```

컨트롤러나 서비스 계층은 현재 사용자를 알지만, 그 정보가 원격 MCP 요청을 만드는 전송 계층까지 저절로 따라가지는 않습니다. `AuthenticationMcpTransportContextProvider`가 현재 인증을 넘겨 주어야 토큰을 고르는 구현체가 알맞은 토큰을 헤더에 붙입니다. 스트리밍 호출은 리액터가 실행 스레드를 보장하지 않아 인증이 끊길 수 있으므로, `contextWrite(AuthenticationMcpTransportContextProvider.writeToReactorContext())`로 인증 문맥을 리액터 컨텍스트에 직접 넣습니다.

### 세 가지 인가 흐름

토큰을 붙이는 확장 지점은 HttpClient 기반이면 `McpSyncHttpClientRequestCustomizer`, WebClient 기반이면 `ExchangeFilterFunction`입니다. 흐름마다 이를 구현한 클래스가 따로 있고, 앞의 등록 ID를 넘겨 빈으로 선언합니다.

**인가 코드 흐름**은 로그인한 사용자를 대신해 호출할 때 씁니다.

<figure class="wide-figure" markdown>
![인가 코드 처리 흐름 시퀀스](../assets/figures/fig5-9.png)
<figcaption>인가 코드 처리 흐름 시퀀스</figcaption>
</figure>

토큰이 없거나 만료된 요청에 서버가 401을 돌려주면(2~3번) 클라이언트가 인가를 시작하고 사용자는 로그인과 동의를 거칩니다(4~6번). 리다이렉트로 받은 인가 코드를 토큰으로 바꾼 뒤(7~9번) 원래 요청을 다시 보냅니다. `client_id`와 `client_secret`은 앱의 자격 증명일 뿐이고, 발급된 토큰은 로그인한 사용자를 대표합니다. 그래서 같은 앱에서도 사용자마다 토큰의 권한이 다릅니다. 요청한 사람의 메일함에만 접근해야 하는 에이전트가 이 흐름에 맞습니다.

**클라이언트 자격 증명 흐름**은 사용자가 없는 시스템 간 호출에 씁니다.

<figure class="wide-figure" markdown>
![클라이언트 자격 증명 처리 흐름 시퀀스](../assets/figures/fig5-10.png)
<figcaption>클라이언트 자격 증명 처리 흐름 시퀀스</figcaption>
</figure>

스케줄러나 이벤트가 작업을 시작하면 앱이 자기 `client_id`와 `client_secret`으로 시스템 토큰을 바로 요청합니다. 인가 코드도 동의 화면도 없습니다. 사내 위키의 공개 문서로 벡터 데이터베이스를 주기적으로 갱신하는 에이전트처럼 사용자별 접근 제어가 필요 없는 작업에 맞습니다.

**하이브리드 흐름**은 두 흐름을 단계별로 이어 붙인 형태입니다.

<figure class="wide-figure" markdown>
![하이브리드 처리 흐름 시퀀스](../assets/figures/fig5-11.png)
<figcaption>하이브리드 처리 흐름 시퀀스</figcaption>
</figure>

1단계에서는 앱이 기동하면서 클라이언트 자격 증명 흐름으로 시스템 토큰을 받아 `initialize`와 `tools/list`를 보냅니다. 2단계에서 사용자의 질문으로 `tools/call`이 필요해지면 인가 코드 흐름으로 사용자 토큰을 받아 호출합니다. HttpClient 기반이라면 `OAuth2HybridSyncHttpRequestCustomizer`에 사용자용과 시스템용 등록 ID를 함께 넘깁니다. 자동 구성의 기동 시점 탐색을 끄지 않고도 사용자 권한으로 툴을 호출할 수 있어, 책은 엔터프라이즈 서비스에 이 흐름을 권합니다.

## 서버 시큐리티 구성

`mcp-server-security`는 스프링 시큐리티 위에서 MCP 서버를 OAuth2 리소스 서버나 API 키 방식으로 보호합니다. 현재는 웹MVC 기반 서버만 지원하고, OAuth2 구성에서는 JWT만 검증합니다. JWT 서명을 검증할 공개키는 보통 `spring.security.oauth2.resourceserver.jwt.issuer-uri`로 인가 서버 메타데이터를 찾아 가져옵니다. 서버 전체를 보호하는 구성은 다음과 같습니다.

```java title="McpServerSecurityConfig.java (책 예제 발췌)"
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http
      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
      .with(
          McpServerOAuth2Configurer.mcpServerOAuth2(),
          mcpAuthorization -> {
            mcpAuthorization.authorizationServer(issuerUri);
            // mcpAuthorization.validateAudienceClaim(true);
            // mcpAuthorization.sessionBinding(Customizer.withDefaults());
          }
      )
      .build();
}
```

익숙한 스프링 시큐리티 설정에 MCP 전용 설정기 `McpServerOAuth2Configurer` 하나를 더한 모습입니다. `issuerUri`에는 앞의 `issuer-uri` 값이 들어갑니다. 이 구성은 초기화부터 툴 호출까지 모든 요청에 유효한 JWT를 요구합니다. 주석으로 둔 옵션은 검증을 더 엄격하게 합니다. `validateAudienceClaim(true)`는 토큰의 `aud` 클레임을 검사해 이 서버용으로 발급된 토큰인지 확인합니다. 이 클레임을 넣지 않는 인가 서버도 있어 기본값은 꺼져 있지만, MCP 인가 명세는 서버가 자기 대상으로 발급된 토큰만 받도록 요구하므로 운영 환경에서는 이 검증을 켜고 인가 서버도 올바른 `aud`를 발급하게 맞추는 편이 안전합니다. `sessionBinding`은 상태를 가진 MCP 세션을 같은 사용자나 자격 증명에 묶어 재사용을 막습니다. 다만 하이브리드 흐름은 단계마다 신원이 달라질 수 있어 끄는 편이 좋습니다.

초기화와 툴 목록 조회는 공개해도 괜찮다면 `/mcp` 진입은 열어 두고 툴 실행만 보호할 수도 있습니다. 이때는 `@EnableMethodSecurity`를 켜고 `@McpTool`이나 `@Tool` 메서드에 `@PreAuthorize`로 스코프나 역할 검사를 붙입니다. 툴 안에서는 `SecurityContextHolder`로 현재 사용자를 꺼내 그 사람의 문서만 조회할 수 있습니다. 폐쇄망의 단순한 서버 간 연동이라면 API 키 인증도 선택지입니다.

## MCP 인가 서버 구성

사용자 인증과 토큰 발급을 MCP 서버 밖의 전용 인가 서버로 분리하면 클라이언트 등록, 토큰 만료 같은 운영 정책을 한곳에서 관리할 수 있습니다. `mcp-authorization-server`는 `McpAuthorizationServerConfigurer`로 스프링 인가 서버를 MCP 규격에 맞게 구성하며, MCP 인가 명세에 정의된 동적 클라이언트 등록과 리소스 지시자를 지원합니다. 설정에서는 `spring.security.oauth2.authorizationserver.client.<client-name>` 아래에 클라이언트마다 `registration.authorization-grant-types`(허용할 인가 흐름), `registration.redirect-uris`(인증 뒤 돌아갈 콜백 주소), `token.access-token-time-to-live`(액세스 토큰 유효 시간, 기본 5분)를 정합니다. 책의 예에는 MCP 인스펙터와 클로드 코드의 콜백 주소도 들어 있습니다.

인가 서버는 별도 스프링 부트 애플리케이션으로 띄웁니다. 기동 뒤 `/.well-known/oauth-authorization-server`에 접속하면 발급자, 토큰 엔드포인트, `jwks_uri` 같은 메타데이터를 확인할 수 있습니다. 이 응답의 `grant_types_supported`는 서버 전체가 지원하는 목록이라 클라이언트 등록에 없는 `refresh_token`도 보입니다. 클라이언트가 실제로 쓸 수 있는 흐름은 등록 설정이 제한합니다.

## 4-티어 아키텍처에서의 위치

4-티어 아키텍처에서 보안은 한 티어에 속하지 않고 모든 계층을 가로지르는 횡단 관심사입니다. 이 글의 구성은 능력 티어(T3)를 로컬과 원격으로 나누는 MCP 경계에 적용됩니다. 클라이언트는 사용자나 시스템의 권한을 토큰에 담아 경계를 넘기고, 원격 MCP 서버는 그 토큰을 스스로 검증한 뒤 툴 실행을 허용합니다. 다음 글부터 6장입니다. [AI 에이전트 서비스 4-티어 아키텍처](../part6/14-four-tier-architecture-for-ai-agent-systems.md)에서 채널, 오케스트레이션, 능력, 파운데이션과 횡단 관심사로 이루어진 전체 구조를 정의합니다.

## 이 장의 실습 프로젝트

!!! example "5.5 MCP 기반 AI 챗봇 CLI 프로젝트"
    3장의 RAG 기능을 MCP 서버로 옮겨 툴, 리소스, 프롬프트, 자동 완성으로 공개하고, 같은 프로젝트의 MCP 클라이언트 CLI가 원격 툴을 `ToolCallback`으로 받아 대화에 씁니다. 같은 서버를 클로드 코드나 코덱스 같은 외부 AI 에이전트에 연결하는 방법은 [부록 글](../appendix/22-openai-evaluation-and-external-agents.md)에서 다룹니다. 전체 실행 과정은 예제 저장소 [`chapter5/`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter5)의 README를 따릅니다.

    ```bash
    cd chapter5
    # 터미널 1: RAG MCP 서버(포트 8085, 엔드포인트 /mcp)
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server --spring.ai.cli.step=ch5-server-step4"
    # 터미널 2: MCP 클라이언트 CLI(ch5-final)
    ./mvnw spring-boot:run
    ```

<figure class="wide-figure" markdown>
![MCP 기반 AI 챗봇 CLI의 처리 흐름 시퀀스](../assets/figures/fig5-12.png)
<figcaption>MCP 기반 AI 챗봇 CLI의 처리 흐름 시퀀스</figcaption>
</figure>

## 책에서 더 다루는 내용

!!! book "책 5.4절"
    - MCP 보안 환경: 환경별 보안 쟁점과 제로 트러스트 관점
    - MCP 클라이언트 시큐리티 공통 설정: 설정 프로퍼티, 전송 계층과 인가 흐름별 구현체 조합
    - MCP 클라이언트 시큐리티 세부 구현: 흐름별 빈 등록, 프로그래밍 방식 클라이언트 생성, 커스텀 헤더 주입
    - MCP 서버 시큐리티 구성: 공개키 설정 방법, 툴별 권한 검사 예제, API 키 기반 보안
    - MCP 인가 서버 구성: 설정 프로퍼티, 공개 클라이언트와 PKCE, 실행 코드와 메타데이터 응답

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [MCP Authorization specification](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization): HTTP 전송의 OAuth 인가 절차를 정의한 MCP 명세
- [Zero Trust Architecture (NIST SP 800-207)](https://doi.org/10.6028/NIST.SP.800-207): NIST의 제로 트러스트 아키텍처 문서
- [spring-ai-community/mcp-security](https://github.com/spring-ai-community/mcp-security): MCP 클라이언트, 서버, 인가 서버 보안 모듈 저장소
