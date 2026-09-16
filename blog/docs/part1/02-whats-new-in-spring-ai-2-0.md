---
title: "스프링 AI 1.0에서 2.0으로, 무엇이 달라졌나"
description: "AI 통합의 1.0에서 에이전트의 2.0으로 진화한 스프링 AI의 주요 변화를 기반 플랫폼, 설정, 대화 메모리, 툴 호출, MCP, 에이전트 지원 순서로 정리합니다."
tags:
  - 1장
---

# 스프링 AI 1.0에서 2.0으로, 무엇이 달라졌나

<div class="post-meta" markdown>
<span class="tier-chip all">4-티어 전체</span> 책 1.3.3절 | 예제 [`basic-chat`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/basic-chat), [`chapter2`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter2), [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

[앞 글](../part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md)에서는 자바 개발자에게 스프링 AI가 왜 필요한지 보았습니다. 이 글은 그 스프링 AI가 1.0에서 2.0으로 오면서 무엇이 바뀌었는지 정리합니다. 책 1.3.3절의 표 1.1을 영역별로 다시 묶고, 변화마다 예제 저장소 코드에서 어떤 모습으로 드러나는지 확인한 뒤, 자세히 다루는 이 사이트의 글로 안내합니다.

책의 설명과 예제, 실습 프로젝트는 모두 스프링 AI 2.0을 기준으로 하며, 로컬에서 띄우기 쉬운 올라마 모델로 실행합니다. 이 사이트의 코드 블록도 책의 예제 저장소에서 가져옵니다.

## AI 통합의 1.0, 에이전트의 2.0

스프링 AI가 내건 목표는 2025년 5월 1.0 정식 버전에서 실제 API로 구현됐습니다. `ChatClient` 하나로 여러 모델 제공자를 다루고, 툴 호출 추상화, `ChatMemory` 기반 대화 메모리, RAG 어드바이저까지 갖추면서 자바 진영을 대표하는 AI 개발 프레임워크로 자리 잡았습니다. MCP 지원도 일찍 시작했고, 이때 만든 코드는 공식 MCP 자바 SDK의 토대가 됐습니다.

아쉬운 점도 있었습니다. 툴을 호출하고 결과를 넣어 다시 모델을 부르는 반복 루프가 모델 구현 안쪽에 감춰져 있었습니다. 반복 횟수를 제한하거나 실행 전에 승인을 받는 것처럼 에이전트에 필요한 세밀한 제어는 개발자가 따로 만들어야 했습니다.

2026년 6월에 나온 2.0 정식 버전은 이 토대를 에이전트 시대에 맞게 다시 짰습니다. 기능 몇 가지를 보탠 정도가 아니라, 플랫폼 기반을 한 세대 올리고 툴 루프를 어드바이저 체인의 구성 요소로 꺼낸 아키텍처 차원의 변화입니다.

## 한눈에 보는 주요 변화

책의 표 1.1을 영역별로 간추리면 다음과 같습니다.

| 영역 | 스프링 AI 1.0 | 스프링 AI 2.0 |
| --- | --- | --- |
| 기반 플랫폼 | 스프링 부트 3.x, Jackson 2, 자바 17 이상 | 스프링 부트 4.x(필수)와 스프링 프레임워크 7, Jackson 3, 자바 17 이상이며 21 권장 |
| 모델 제공자 | `ChatClient` 하나로 제공자 20곳 지원 | 코어는 오픈AI, 앤트로픽, 올라마를 포함한 주요 7곳에 집중 |
| 옵션과 설정 | 옵션은 세터로 수정, 설정 키 구조가 복잡 | 옵션은 불변이라 `mutate()`로 변경, 설정 키는 평평하게 정리 |
| 대화 메모리 | Prompt 방식과 Message 방식이 함께 존재 | `MessageChatMemoryAdvisor` 하나로 정리, 대화 ID는 호출할 때 지정 |
| 툴 호출 | 플래그로 실행을 제어하고 빈 이름 문자열로 참조 | 툴은 `ToolCallback` 객체로 등록, 루프는 재귀적 어드바이저 `ToolCallingAdvisor`가 제어 |
| MCP | 초기 지원이 여러 곳에 흩어져 있음 | 코어에 정식 포함, 애너테이션으로 선언, OAuth 기반 보안과 연동 |
| 에이전트 지원 | 기본 구성 요소 제공 | 툴 루프 제어 훅과 동적 툴 탐색 확충 |
| 플랫폼 지원 수명 | 스프링 부트 3.x의 OSS 지원은 2026년 6월 종료 | 스프링 부트 4.x가 현재 OSS 지원을 받는 세대, 6개월마다 마이너 릴리스 |

## 기반 플랫폼과 모델 제공자

가장 먼저 눈에 띄는 변화는 플랫폼 기반입니다. 2.0은 스프링 부트 4.x와 스프링 프레임워크 7, Jackson 3 위에서 동작합니다. 자바는 17 이상이면 되지만 21을 권장하고, 책 2장은 2.0이 자바 21 이상의 최신 기능을 적극적으로 쓴다고 설명합니다. 예제 저장소의 장별 `pom.xml`에서 이 기준을 확인할 수 있습니다.

```xml title="pom.xml"
--8<-- "chapter2/pom.xml:7:12"
--8<-- "chapter2/pom.xml:20:23"
--8<-- "chapter2/pom.xml:38:48"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/pom.xml)</span>

부모 POM은 스프링 부트 4.0.7, 자바 버전은 21, `spring-ai.version`은 2.0.0입니다. `dependencyManagement`에서 `spring-ai-bom`을 가져오기 때문에 `spring-ai-starter-model-ollama` 같은 스타터에는 버전을 적지 않습니다. `basic-chat`부터 `chapter6`까지 이 세 값이 모두 같습니다.

모델 제공자 쪽에서는 방향이 바뀌었습니다. 1.0이 `ChatClient` 하나로 제공자 20곳을 지원했다면, 2.0 코어는 오픈AI, 앤트로픽, 올라마를 비롯한 주요 7곳에 집중합니다. 애플리케이션 코드는 `ChatModel`, `ChatClient` 같은 공통 인터페이스에 기대므로, 제공자를 올라마에서 오픈AI로 바꿀 때도 주로 스타터와 설정만 손봅니다. 공통 인터페이스의 설계는 [ChatModel과 ChatClient 글](../part2/03-chatmodel-chatclient-and-prompt-engineering.md)에서, 오픈AI로 실행하는 방법은 [부록 글](../appendix/22-openai-evaluation-and-external-agents.md)에서 다룹니다.

## 옵션과 설정

1.0에서는 옵션 객체를 세터로 고쳤고, 설정 파일의 키 구조도 복잡했습니다. 2.0의 옵션 객체는 불변입니다. 값을 바꿀 때는 `mutate()`로 기존 값을 담은 빌더를 얻고, 필요한 값만 바꿔 새 옵션 객체를 만듭니다. 설정 키도 알아보기 쉬운 평평한 구조로 정리됐습니다.

```yaml title="application.yml (chapter2)"
--8<-- "chapter2/src/main/resources/application.yml:4:15"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/src/main/resources/application.yml)</span>

채팅 모델 제공자는 `spring.ai.model.chat`으로 고르고, 올라마 서버 주소는 `spring.ai.ollama.base-url`에 둡니다. 모델 이름(`model`)과 추론 사용 여부(`think`)는 `spring.ai.ollama.chat` 바로 아래에 나란히 적습니다. 오픈AI로 실행할 때도 같은 모양으로 `spring.ai.model.chat`을 `openai`로 바꾸고 API 키와 모델 이름을 적습니다.

불변 옵션은 6장 코드에서 볼 수 있습니다. [`OrchestrationToolCallingAdvisor`](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java#L139-L145)는 매 라운드 요청에 메타 툴을 더할 때 기존 옵션을 직접 고치지 않습니다. `mutate()`로 빌더를 받아 툴 목록만 바꾼 새 옵션을 만들고, 요청 객체도 같은 방식으로 새로 만듭니다. `ChatOptions`의 쓰임새는 [ChatModel과 ChatClient 글](../part2/03-chatmodel-chatclient-and-prompt-engineering.md)에서 이어집니다.

## 대화 메모리

1.0에는 Prompt 방식과 Message 방식의 메모리 어드바이저가 함께 있었습니다. 2.0은 저장된 대화를 메시지 객체 목록 그대로 프롬프트에 넣는 `MessageChatMemoryAdvisor` 하나로 정리했고, 대화 ID는 호출하는 순간에 넘기도록 바꿨습니다. 어느 대화의 기록을 읽고 쓸지 요청마다 분명해져 여러 사용자의 대화가 뒤섞이는 사고를 줄일 수 있습니다. 어떤 사용자가 어느 대화 ID를 쓸 수 있는지 확인하는 일은 애플리케이션의 몫입니다.

```java title="Ch2Step4_Memory.java"
--8<-- "chapter2/src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java:31:67"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java)</span>

생성자에서 최근 20개 메시지를 유지하는 `MessageWindowChatMemory`를 만들고, 이를 쓰는 `MessageChatMemoryAdvisor`를 기본 어드바이저로 한 번 등록합니다. 대화 ID는 실행할 때 만든 UUID 하나를 계속 쓰고, 질문을 보낼 때마다 `advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))`로 이번 요청이 속한 대화를 알려 줍니다. 어드바이저는 빌드할 때 등록하고 요청마다 파라미터만 바꾸는 이 배치는 책 2.8.1절이 권하는 방식이기도 합니다. 메모리 저장소와 어드바이저 실행 순서는 [대화 메모리와 어드바이저 체인 글](../part2/05-chat-memory-and-advisor-chain.md)에서 자세히 봅니다.

## 툴 호출: 어드바이저가 제어하는 툴 루프

책이 가장 핵심적인 변화로 꼽는 영역입니다. 1.0에서는 플래그로 툴 실행을 제어하고 빈 이름 문자열로 툴을 가리켰습니다. 2.0은 툴을 `ToolCallback` 객체 단위로 등록하는 쪽으로 정리됐다고 책은 표 1.1에서 요약합니다.

루프를 맡는 주체도 바뀌었습니다. 이제는 어드바이저 체인 안의 `ToolCallingAdvisor`가 루프를 돌립니다. 재귀적 어드바이저인 이 컴포넌트는 툴 실행, 결과 주입, 모델 재호출을 최종 답이 나올 때까지 되풀이합니다. 모델 구현 속에 묻혀 있던 루프가 다른 어드바이저와 조합할 수 있는 부품이 되면서, 반복 횟수 제한이나 사용자 승인 게이트 같은 제어 로직을 붙이기 쉬워졌습니다. 2.0에서는 따로 등록하지 않아도 툴 루프가 자동으로 돌지만, 체인 속 위치와 동작을 정하고 싶으면 직접 구성합니다.

```java title="AgentConfig.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java:174:192"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java)</span>

위 코드는 6장 메인 에이전트가 요청마다 루프 어드바이저를 만드는 팩토리입니다. 기본 경로는 `ToolCallingAdvisor.builder()`에 두 설정을 더합니다. `toolExecutionEligibilityChecker`에는 한 요청의 툴 실행 라운드가 상한(`MAX_TOOL_ROUNDS`, 10)을 넘으면 루프를 멈추는 안전 가드를 넣고, `advisorOrder`로 체인 안의 순서를 고정합니다. 툴이 많아 동적 툴 탐색을 켜는 경우에는 같은 설정을 `ToolSearchToolCallingAdvisor` 빌더에 적용합니다. 어드바이저를 요청마다 새로 만드는 이유는 라운드 카운터를 다른 요청과 나눠 쓰지 않기 위해서입니다. 툴을 정의하는 방식은 [툴 호출 설계 글](../part4/09-tool-calling-design-in-spring-ai.md)과 [툴 구현과 실행 제어 글](../part4/10-tool-implementation-and-execution-control.md)에서, 재귀 루프의 흐름은 [재귀적 어드바이저 글](../part6/16-recursive-advisor-and-tool-loop-control.md)에서 다룹니다.

## MCP와 에이전트 지원

책 표 1.1은 MCP 지원이 1.0 시절 여러 곳에 흩어져 있다가 2.0에서 코어에 정식으로 자리 잡았다고 정리합니다. 서버 기능은 애너테이션으로 선언합니다. 6장 운영 서버의 `OperationsMcpTools`는 `@McpTool`을 붙인 메서드로 재고 조회와 발주 툴을 노출합니다. 보안은 스프링 AI 커뮤니티의 mcp-security 프로젝트가 맡습니다. 이 프로젝트로 MCP 서버를 OAuth2 리소스 서버로 구성하면, 서버는 요청의 JWT를 스스로 검증한 뒤에만 툴을 실행합니다. MCP 클라이언트는 [MCP 기본 글](../part5/11-mcp-basics-and-spring-ai-mcp-client.md), 서버는 [MCP 서버 만들기 글](../part5/12-building-an-mcp-server-with-spring-ai.md), 보안은 [MCP 보안 글](../part5/13-mcp-security-oauth2-and-jwt.md)에서 봅니다.

에이전트를 위한 지원도 넓어졌습니다. 1.0이 에이전트를 만들 기본 구성 요소를 제공했다면, 2.0은 여기에 툴 루프 제어 훅과 동적 툴 탐색을 더했습니다. `ToolCallingAdvisor`는 루프 진입 직전, 반복마다 하위 체인을 부르기 전후, 루프 종료 직후에 끼어들 수 있는 훅 메서드를 제공합니다. 개발자는 이 훅을 재정의해 반복 상한이나 비용 한도 같은 안전 장치를 둡니다. 앞에서 본 `OrchestrationToolCallingAdvisor`도 `doBeforeCall`, `doBeforeStream` 훅을 재정의해 메타 툴을 매 라운드 노출합니다. 툴 루프 코드에 나온 `ToolSearchToolCallingAdvisor`는 동적 툴 탐색을 맡습니다. 모든 툴 정의를 매번 모델에 보내는 대신 툴을 찾는 툴 하나만 보여 주고, 모델이 검색한 툴의 정의만 컨텍스트에 더합니다. 훅은 [재귀적 어드바이저 글](../part6/16-recursive-advisor-and-tool-loop-control.md)에서, 동적 툴 탐색은 [에이전트 아키텍처와 동적 툴 탐색 글](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md)에서, 위험한 작업 앞에 승인을 받는 방법은 [사용자 개입(HITL) 글](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)에서 다룹니다.

## 4-티어 아키텍처에서의 위치

2.0의 변화는 한 계층에 머물지 않습니다. `ToolCallingAdvisor`와 `MessageChatMemoryAdvisor`는 루프와 상태를 관리하는 T2 오케스트레이션의 뼈대가 되고, `ToolCallback`과 코어에 들어온 MCP는 T2가 T3 능력을 호출하는 방식을 하나로 맞춥니다. 모델 제공자 지원은 T4 파운데이션의 모델 자원에 닿고, mcp-security의 OAuth2 구성은 횡단 관심사인 보안에 속합니다. 다음 글부터는 책 2장을 따라 이 부품들을 차례로 만들어 봅니다. 첫 순서는 [ChatModel과 ChatClient, 그리고 프롬프트 엔지니어링](../part2/03-chatmodel-chatclient-and-prompt-engineering.md)입니다.

## 책에서 더 다루는 내용

!!! book "책 1.3.3절"
    - **1.3.3 스프링 AI의 진화:** 1.0이 자바 진영의 대표 AI 프레임워크로 자리 잡은 배경과 2.0이 에이전트 시대에 맞춰 설계를 바꾼 방향
    - **표 1.1 스프링 AI 1.0과 2.0의 주요 변화:** 이 글의 표가 간추린 영역별 비교의 원문
    - **장별 상세:** 옵션과 설정은 2.4절, 대화 메모리와 어드바이저는 2.7~2.8절, 툴 실행 제어는 4.4절, MCP 서버와 보안은 5.3~5.4절, 재귀적 어드바이저는 6.3절, 동적 툴 탐색은 6.4.5절

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [Spring AI 2.0.0 GA Available Now](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now): 스프링 AI 2.0 정식 버전 출시 발표
