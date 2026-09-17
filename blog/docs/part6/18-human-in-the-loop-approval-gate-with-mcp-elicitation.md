---
title: "사용자 개입(HITL): MCP Elicitation으로 만드는 승인 게이트"
description: "위험한 툴 실행 전에 사람이 개입하는 사용자 개입 워크플로(HITL)를 MCP Elicitation으로 구현합니다. 툴 루프 안쪽의 승인 게이트 구조와 서버와 클라이언트 양쪽의 코드를 다룹니다."
tags:
  - 6장
---

# 사용자 개입(HITL): MCP Elicitation으로 만드는 승인 게이트

<div class="post-meta" markdown>
<span class="tier-chip tx">횡단 관심사 거버넌스</span> 책 6.4.4절, 5.3.6절, 6.6.4절 | 예제 [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

[앞 글](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md)에서 만든 에이전트는 로컬 툴과 MCP 서버의 원격 툴을 같은 방식으로 호출합니다. 모델이 툴을 고르면 곧바로 실행되는 구조입니다. 조회라면 괜찮지만 시스템 설정 변경, 결제, 외부 발송처럼 되돌리기 어려운 작업까지 모델의 판단에만 맡겨 둘 수는 없습니다.

이런 작업 앞에서는 에이전트가 멈춰서 사람의 입력을 기다리고, 명시적인 승인을 받은 뒤에 작업을 이어 가야 합니다. 책에서는 이 패턴을 사용자 개입 워크플로(HITL)라고 부릅니다. 이 글은 MCP의 추가 정보 요청(Elicitation)으로 승인 게이트를 만들고, 예제 저장소의 발주 툴에 적용합니다.

## 승인 게이트를 에이전트 쪽에 두는 이유

책 5장의 MCP 서버 예제(5.3.6절)에서도 이 기능을 다뤘습니다. 그 절의 분석 툴([MCP 서버 만들기](../part5/12-building-an-mcp-server-with-spring-ai.md) 글에서는 상자에서만 언급)이 실행 도중 `exchange.createElicitation(...)`으로 분석을 계속할지 물었고, 클라이언트의 `@McpElicitation` 핸들러가 답했습니다. 다만 그때 핸들러는 프로토콜 동작을 확인하려는 용도였기 때문에, 사람에게 묻지 않고 승인(`ACCEPT`)을 바로 돌려주는 자동 응답 스텁이었습니다.

에이전트를 만들 때는 바로 이 핸들러가 중요해집니다. 지금 만드는 에이전트가 MCP 호스트이고, 그 안의 MCP 클라이언트로 서버에 연결하기 때문입니다. 외부 MCP 서버의 툴은 다른 팀이나 외부 생태계에서 만들 수 있어서 그 코드에 손대기 어렵습니다. 하지만 그 툴이 보낸 승인 요청에 답하는 핸들러는 온전히 에이전트 쪽 코드입니다. 이 핸들러를 실제 사람에게 묻고 거절도 할 수 있는 승인 게이트로 바꾸면 됩니다. 승인 요청은 작업 코드가 실행되기 직전에 도착하므로 개입하기 알맞은 지점입니다. 다만 이 게이트는 승인을 묻는 툴에만 닿습니다. 묻지 않는 서버라면 호스트가 `tools/call`을 보내기 전에 자체 정책으로 걸러야 합니다.

## 툴 루프 안의 승인 게이트

<figure class="wide-figure" markdown>
![스프링 AI 에이전트 툴 루프 내부의 승인 게이트](../assets/figures/fig6-16.png)
<figcaption>스프링 AI 에이전트 툴 루프 내부의 승인 게이트</figcaption>
</figure>

그림은 5장의 분석 툴을 예로 든 흐름입니다. 모델이 툴 호출을 요청하면 툴 루프(`ToolCallingAdvisor`)가 MCP 서버에 `tools/call`을 보냅니다. 서버 툴은 로직을 실행하기 전에 `createElicitation`을 호출하고 응답이 올 때까지 멈춥니다. 요청을 받은 `@McpElicitation` 핸들러는 콘솔에 승인 요청을 출력하고 사용자의 y 또는 n 입력을 기다립니다.

사용자가 n을 입력하면 핸들러는 `DECLINE`을 돌려주고, 서버 툴은 분석 로직을 실행하지 않은 채 중단 응답을 반환합니다. 툴 루프는 거절로 실행되지 않았다는 결과를 모델에 전달합니다. y를 입력하면 `ACCEPT`와 함께 `confirmed=true`가 전달되고, 서버는 남은 로직을 실행해 최종 결과를 반환합니다. 어느 쪽이든 모델은 툴 결과를 받아 다음 추론을 이어 갑니다. 에이전트가 사용자와 외부 툴 사이에서 호출 승인 관문을 맡는 구조입니다.

## MCP Elicitation 프리미티브

MCP 서버는 자기 기능을 노출하는 데서 그치지 않고, 연결된 클라이언트의 기능에 기대어 동작하기도 합니다. 툴을 처리하는 도중에 쓰는 기능은 성격에 따라 둘로 나뉩니다. 로깅, 진행률 알림, 핑은 서버 상태를 알리는 통보형이고, 샘플링과 추가 정보 요청은 클라이언트의 기능을 거꾸로 호출하는 상호작용형입니다. 추가 정보 요청(Elicitation)은 툴 실행 중에 클라이언트 쪽 사용자에게 입력을 받는 기능입니다.

서버가 보내는 요청에는 메시지와 함께, 받고 싶은 입력의 형식이 JSON 스키마(`requestedSchema`)로 담깁니다. 이렇게 폼을 함께 보내는 요청이 `ElicitFormRequest`이고, `ElicitRequest`의 한 종류입니다. 클라이언트는 `ElicitResult`로 답합니다. 액션은 승인(`ACCEPT`), 거절(`DECLINE`), 취소(`CANCEL`) 세 가지이고, 스키마에 입력 필드가 있으면 그 형식에 맞춘 값을 `content` 맵에 담아 보냅니다. 서버 툴은 돌아온 액션을 보고 작업을 계속할지 멈출지 정합니다.

이 기능은 양방향 통신을 전제로 합니다. 서버가 요청을 보내도 클라이언트가 받을 준비를 해 두지 않았다면 동작하지 않습니다. 또 스프링 AI의 무상태 MCP 서버(`protocol=STATELESS`)는 연결을 유지하지 않으므로 클라이언트에 요청을 보낼 수 없습니다. 예제의 운영 서버는 `ops` 프로파일에서 `STREAMABLE` 프로토콜로 실행됩니다.

```yaml title="application-ops.yml"
--8<-- "chapter6/src/main/resources/application-ops.yml:1:18"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/resources/application-ops.yml)</span>

운영 서버는 8085 포트를 쓰는 서블릿 웹 애플리케이션이고, 이 프로파일에서는 MCP 클라이언트를 끄고 서버만 켭니다. MCP 엔드포인트는 `/mcp`입니다.

## 서버 쪽 구현: 실행 직전에 승인 요청

운영 서버의 `OperationsMcpTools`는 재고를 조회하는 `check_stock`과 발주하는 `place_purchase_order`를 `@McpTool`로 노출합니다. `place_purchase_order`는 실제 발주를 흉내 낸 모의(mock) 툴로, 모델이 골랐다고 해서 바로 실행되면 곤란한 작업의 예입니다.

```java title="OperationsMcpTools.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java:40:58"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java)</span>

메서드는 파라미터로 받은 `McpSyncServerExchange`로 `createElicitation()`을 호출합니다. 요청 메시지에는 어떤 SKU를 몇 개 발주하는지 담고, 스키마는 속성이 없는 빈 객체로 둡니다. 이번에는 예, 아니오만 확인하면 되기 때문입니다. 툴은 응답이 올 때까지 기다리고, 액션이 `ACCEPT`가 아니면 재고를 건드리지 않고 취소 메시지를 돌려줍니다. 승인됐을 때만 재고를 늘리고 완료 메시지를 반환합니다. 사용자가 개입하는 시점은 모델이 답을 만드는 도중이 아니라 실제 작업이 실행되기 바로 앞입니다.

## 클라이언트 쪽 구현: @McpElicitation 핸들러

서버가 보낸 승인 요청은 MCP 경계를 거슬러 에이전트 쪽으로 올라옵니다. 클라이언트에서는 `@McpElicitation`을 붙인 메서드 하나로 이 요청을 받습니다.

```java title="ConsoleElicitationHandler.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java:23:43"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java)</span>

핸들러는 서버의 메시지를 콘솔에 보여 주고 사용자 입력을 읽습니다. y일 때만 `ACCEPT`를 돌려주고, 다른 값을 입력했거나 입력이 없으면 `DECLINE`으로 처리합니다. 기본값을 거절로 두었으니 사용자가 분명히 승인한 경우에만 위험한 작업이 실행됩니다. `Scanner`를 필드 하나로 공유하는 이유는 호출할 때마다 새로 만들면 콘솔 입력이 유실되기 때문입니다. `@Profile("!ops & !knowledge")`는 이 핸들러가 서버가 아닌 클라이언트 실행에서만 등록되게 합니다.

`clients = "operations"`의 `operations`는 클라이언트 `application.yml`의 `spring.ai.mcp.client.streamable-http.connections` 아래에 등록한 운영 서버 연결 이름입니다. 이 설정 덕분에 운영 서버 연결에서 올라온 요청만 이 핸들러가 처리하고, 다른 MCP 서버를 더 붙여도 연결 이름으로 승인 처리 범위를 나눌 수 있습니다. 서버가 늘어나도 승인 정책은 에이전트 쪽 핸들러 한곳에 모입니다. 웹 서비스라면 콘솔 대신 SSE나 웹소켓으로 프런트엔드에 승인 창을 띄우고 비동기로 응답을 기다리도록 핸들러를 확장하면 됩니다.

## 실행 예: 발주 전 승인

Step 3 러너(`Ch6Step3_ApprovalGate`)는 코어 에이전트에 "SKU-300을 10개 발주해줘"라고 요청합니다. 운영 서버를 먼저 띄운 뒤 다른 터미널에서 클라이언트를 실행합니다.

```bash
# 터미널 1: 운영 MCP 서버
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=ops"
# 터미널 2: 클라이언트
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-step3"
```

책에 실린 실행 결과를 요약하면 이렇습니다. 모델은 `place_purchase_order`를 SKU-300, 수량 10으로 호출합니다. 툴 결과가 돌아오기 전에 콘솔에 `[승인 요청] place_purchase_order 툴로 SKU-300 10개를 실제로 발주합니다. 진행할까요? (y/n)`이 먼저 뜹니다. y를 입력하면 "SKU-300 10개를 발주했습니다. (승인 완료)"라는 툴 결과가 돌아오고, 모델이 이를 받아 작업을 요약합니다. n을 입력했다면 서버는 발주를 취소하고 재고도 바꾸지 않습니다.

이 흐름에서 모델은 무엇을 할지 결정할 뿐이고, 실제로 실행할 권한은 MCP 서버와 사용자 승인이 함께 갖습니다. 툴 호출과 상태 변경 사이에 사람의 승인이라는 경계가 생긴 것입니다. 코어 에이전트의 툴 루프에는 한 요청의 툴 실행 라운드를 10회로 제한하는 안전 가드도 걸려 있어, 발주 같은 작업을 다루는 루프가 끝없이 도는 일을 막습니다. 최종 요약 문장은 모델이 매번 새로 쓰기 때문에 실행할 때마다 표현이 달라질 수 있고, 작은 로컬 모델에서는 SKU 표기나 조사가 흔들리기도 합니다. 확인할 것은 문장이 아니라 승인을 거쳐 실행되는 흐름입니다.

## 4-티어 아키텍처에서의 위치

승인 게이트는 한 티어에 속하지 않고 여러 계층을 가로지르는 횡단 관심사이며, 그중 거버넌스에 해당합니다. 승인 요청은 T3 능력 계층의 MCP 서버 툴에서 출발하고, T2 오케스트레이션의 툴 루프가 결과를 기다리는 동안 T1 채널의 사용자에게 전달됩니다. 예제에서도 요청을 보내는 `OperationsMcpTools`는 `capability/remote` 패키지에, 요청에 답하는 `ConsoleElicitationHandler`는 `channel` 패키지에 있습니다. 다음 글에서는 커뮤니티 라이브러리로 에이전트의 능력을 넓히는 첫 단계로 에이전트 스킬을 봅니다: [에이전트 스킬: 범용 스킬로 에이전트의 능력을 확장하기](../part6/19-agent-skills-extending-capabilities.md)

## 책에서 더 다루는 내용

!!! book "책 6.4.4절, 5.3.6절, 6.6.4절"
    - 5장 분석 툴의 자동 응답 핸들러를 콘솔 승인 게이트로 바꾸는 과정과, `requestedSchema`로 폼 입력을 돌려받는 경우
    - MCP 서버의 클라이언트 의존 기능 전반: 루트 변경 감시, 로깅, 진행률 알림, 핑, 샘플링
    - `McpToolUtils.getMcpExchange()`로 프로그래밍 방식 툴에서 익스체인지를 꺼내고 클라이언트 지원 여부를 확인하는 방법
    - 운영 서버와 지식 서버를 프로파일로 나누는 설정과, 원격 툴이 코어 에이전트에 합류하는 Step 2 실행 결과
    - 코어 기능만으로 만든 에이전트를 상용 에이전트 제품과 비교했을 때 남는 빈자리와 커뮤니티 툴로 넘어가는 흐름

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }
