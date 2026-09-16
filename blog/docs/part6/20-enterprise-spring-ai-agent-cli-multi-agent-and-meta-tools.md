---
title: "엔터프라이즈 스프링 AI 에이전트 CLI: 멀티 에이전트와 메타 툴 오케스트레이션"
description: "코어 에이전트에 MCP 서버, 하위 에이전트, 메타 툴을 차례로 더해 강화 에이전트로 키우는 6장 실습 프로젝트를 코드로 따라갑니다."
tags:
  - 6장
---

# 엔터프라이즈 스프링 AI 에이전트 CLI: 멀티 에이전트와 메타 툴 오케스트레이션

<div class="post-meta" markdown>
<span class="tier-chip t2">T2 오케스트레이션</span><span class="tier-chip t3">T3 능력</span> 책 6.5.2~6.5.4, 6.6.3~6.6.6절 | 예제 [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

6장의 마지막 실습에서는 앞선 장에서 익힌 ChatClient와 어드바이저 체인, RAG, 툴 호출, MCP를 하나의 에이전트 시스템으로 묶습니다. 기능 하나를 확인하는 데모가 아니라, 여러 팀이 능력을 나눠 맡고 함께 운영할 수 있는 엔터프라이즈 에이전트 CLI가 목표입니다.

이 글은 조립 순서를 따라갑니다. 스프링 AI 모듈만으로 코어 에이전트를 만들고, 운영 서버와 지식 서버를 MCP로 연결하고, 하위 에이전트로 멀티 에이전트를 구성한 뒤, 메타 툴로 오케스트레이션을 넓혀 강화 에이전트를 완성합니다. 각 단계는 `spring.ai.cli.step` 값(`ch6-step1`부터 `ch6-step6`, `ch6-final`)으로 골라 실행합니다.

에이전트 구조와 동적 툴 탐색은 [스프링 AI 에이전트 아키텍처 글](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md)에서, 승인 게이트는 [사용자 개입(HITL) 글](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)에서, 스킬은 [에이전트 스킬 글](../part6/19-agent-skills-extending-capabilities.md)에서 다뤘습니다. 여기서는 그 부품들이 어떻게 이어지는지에 집중합니다.

## 코어 에이전트와 에이전트 루프

Step 1의 코어 에이전트 `SpringAIAgent`는 커뮤니티 라이브러리나 백엔드 서버 없이 스프링 AI 모듈만으로 만듭니다. 시간 조회, 계산, 재고 조회와 예약 같은 로컬 툴 일곱 개로 요청을 끝까지 처리하는 가장 작은 완성형입니다. 생성자로 시스템 프롬프트, 툴 목록(`List<ToolCallback>`), 툴 루프 어드바이저 팩토리를 받기 때문에 뒤에서 툴과 서버가 늘어나도 이 클래스는 고칠 필요가 없습니다.

<figure class="wide-figure" markdown>
![코어 에이전트의 에이전트 루프](../assets/figures/fig6-25.png)
<figcaption>코어 에이전트의 에이전트 루프</figcaption>
</figure>

모델은 다음 행동을 추론하고, 툴을 호출하고, 결과를 관찰하는 일을 더 부를 툴이 없을 때까지 되풀이합니다. 이 반복은 개발자가 직접 구현하지 않고, 어드바이저 체인 안의 `ToolCallingAdvisor`가 맡습니다. 대화 메모리를 맡는 `MessageChatMemoryAdvisor`는 루프에 들어가기 전과 나온 뒤에 요청마다 한 번씩만 동작하고, 루프 안에서 쌓이는 툴 호출 기록은 `ToolCallingAdvisor`가 관리합니다.

안전 가드도 둡니다. `AgentSafety`는 `ToolExecutionEligibilityChecker`로 한 요청 안의 툴 실행 라운드를 세고, 상한(10회)을 넘으면 루프를 멈춥니다. 루프 어드바이저를 요청마다 새로 만드는 것도 이 카운터를 요청끼리 공유하지 않기 위해서입니다. 툴이 임계값(기본 10개)만큼 늘어나면 루프 어드바이저를 `ToolSearchToolCallingAdvisor`로 바꿔, 필요한 툴만 검색해 모델에 보여 줍니다.

## MCP 서버로 능력 넓히기

같은 프로젝트를 프로파일로 나눠 MCP 서버 두 개를 띄웁니다. 운영 서버(`ops`, 8085 포트)는 재고 조회 `check_stock`과 발주 `place_purchase_order`를, 지식 서버(`knowledge`, 8086 포트)는 사내 문서 질의에 답하는 `rag_answer_question`을 노출합니다.

클라이언트는 `spring.ai.mcp.client.streamable-http.connections` 아래에 `operations`, `knowledge`처럼 이름 붙인 연결을 두고 `toolcallback.enabled`를 켭니다. 그러면 서버 툴이 `SyncMcpToolCallbackProvider`로 들어오고, 에이전트 구성은 이를 `ObjectProvider`로 받아 로컬 툴과 한 목록에 합칩니다. MCP 클라이언트를 끄면 로컬 툴만으로 돌고, 연결을 켜 두었으면 그 서버가 먼저 떠 있어야 원격 툴이 같은 인터페이스로 합류합니다. 운영 서버를 연결한 Step 2에서 에이전트 코드는 그대로인데 툴은 로컬 7개와 원격 2개를 합쳐 9개가 됩니다. 발주 직전에 사람의 승인을 받는 Step 3은 [승인 게이트 글](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)에서 설명합니다.

지식 서버의 툴은 단순한 함수가 아닙니다.

```java title="KnowledgeMcpTools.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/KnowledgeMcpTools.java:33:53"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/KnowledgeMcpTools.java)</span>

툴 메서드는 `RagAnswerService`에 질문을 넘기기만 합니다. 서비스가 서버의 벡터 스토어에서 문서를 찾고, 서버 안의 `ChatClient`가 찾은 문서만 근거로 답을 완성해 돌려줍니다. 메인 에이전트는 툴을 한 번 호출했을 뿐이지만 MCP 경계 너머에서는 별도의 에이전트가 검색하고 답을 씁니다. 에이전트를 툴로 제공하는(agent as a tool) 이 방식이 멀티 에이전트의 첫 번째 경로입니다(Step 4).

## 하위 에이전트로 멀티 에이전트 확장

Step 4부터 클라이언트는 같은 `SpringAIAgent` 클래스에 커뮤니티 능력을 더한 강화 에이전트(`enhancedAgent` 빈)로 바뀝니다. 멀티 에이전트의 두 번째 경로는 클라이언트 쪽에서 선언하는 하위 에이전트입니다(Step 5).

`ChatClient`를 `@Tool` 메서드로 감싸면 하위 에이전트를 직접 만들 수 있습니다. 하지만 에이전트를 추가할 때마다 코드를 쓰고 다시 컴파일해야 하고, 컨텍스트 격리, 모델 라우팅, 무한 위임 방지, 비동기 실행도 손수 챙겨야 합니다. 스프링 AI 커뮤니티 라이브러리 `spring-ai-agent-utils`의 `TaskTool`은 이 반복 작업을 표준화합니다. 하위 에이전트 호출을 `Task`라는 툴 하나로 노출하고, 에이전트는 마크다운 파일로 정의합니다.

```markdown title="report-writer.md"
--8<-- "chapter6/src/main/resources/agents/report-writer.md:1:13"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/resources/agents/report-writer.md)</span>

프런트매터의 `name`과 `description`은 필수이고, `tools`와 `model`로 쓸 툴과 모델을 지정할 수도 있습니다. 그 아래 본문은 하위 에이전트에게 주는 시스템 프롬프트로 쓰입니다. 정의 파일들은 `Task` 툴 설명에 사용 가능한 에이전트 목록으로 들어가고, 모델은 `subagent_type` 파라미터 값으로 하나를 고릅니다. 그래서 하위 에이전트를 여럿 두어도 등록되는 툴은 `Task` 하나입니다. 정의 디렉터리는 클래스패스가 아닌 `src/main/resources/agents` 같은 파일 시스템 경로로 넘깁니다.

하위 에이전트는 격리된 컨텍스트에서 일하고 핵심 결과만 돌려주므로 메인 에이전트의 대화 기록이 불어나지 않습니다. `TaskTool`은 하위 에이전트가 다시 위임하지 못하게 해서 계층을 두 단계로 묶어 두고, 오래 걸리는 위임은 백그라운드에서 실행한 뒤 `TaskOutputTool`로 결과를 가져오는 방식도 지원합니다. Step 5에서 메인 에이전트는 보고서를 직접 쓰지 않고 `report-writer`에 맡긴 뒤 받은 결과를 전달합니다.

## 메타 툴: 질문하는 툴과 작업 계획 툴

강화 에이전트의 툴은 두 묶음입니다. 재고 조회나 지식 질의처럼 업무를 직접 처리하는 도메인 툴, 그리고 툴 검색, 위임, 계획, 질문처럼 일하는 방식을 다루는 메타 툴입니다. `toolSearchTool`, `TaskTool`, `SkillsTool`과 함께 다음 두 툴이 메타 툴에 속합니다.

`AskUserQuestionTool`은 클로드 코드의 AskUserQuestion을 스프링 AI로 옮긴 커뮤니티 툴입니다. 요청이 모호하면 모델이 스스로 판단해 이 툴을 호출하고, 임의로 가정하는 대신 선택지를 붙인 질문을 사용자에게 보냅니다. 질문은 본문, 짧은 헤더, 설명이 딸린 선택지, 다중 선택 여부로 구성되고, 화면에 띄우는 일은 `QuestionHandler` 구현이 맡습니다. 콘솔이면 `CommandLineQuestionHandler`로 충분하고, 웹이면 SSE나 웹소켓으로 질문을 보내고 답을 비동기로 기다리게 구현합니다. MCP를 쓰지 않고 같은 JVM 안에서 동작하므로 가볍습니다. 이기종 클라이언트와 표준 방식으로 주고받아야 하는 위험 작업 승인은 MCP Elicitation에, 요청을 구체화하는 명확화 질문은 이 툴에 맡기는 식으로 함께 쓸 수 있습니다.

`TodoWriteTool`은 클로드 코드가 쓰는 할 일 목록 방식(TodoWrite)을 스프링 AI로 가져온 툴입니다. 단계가 많은 작업에서 에이전트는 중간 단계를 빠뜨리거나 되풀이하거나 계획에서 벗어나기 쉽습니다. 이 툴은 계획을 모델 안에만 두지 않고 툴 호출로 기록하게 합니다. 모델은 할 일 목록을 만든 뒤 항목 상태를 `pending`에서 `in_progress`, `completed`로 바꿔 가며 작업하고, `in_progress` 항목은 한 번에 하나만 둘 수 있습니다. 목록이 바뀔 때마다 `TodoEventHandler`가 불리므로 진행 상황을 화면이나 모니터링 시스템으로 보낼 수 있습니다. 갱신 기록이 다음 루프에서도 보이도록 대화 메모리와 함께 씁니다.

## 메타 툴 기반 오케스트레이션

Step 6은 `metaTools()` 빈에 `TaskTool`, `SkillsTool`, `TodoWriteTool`, `AskUserQuestionTool`을 모아 메타 툴 묶음을 완성합니다. 빌더가 곧바로 `ToolCallback`을 돌려주는 앞의 두 툴과 달리 뒤의 두 툴은 `@Tool` 객체라 `ToolCallbacks.from()`으로 바꿔 한 목록에 넣습니다. 강화 에이전트는 도메인 툴 묶음과 메타 툴 묶음을 따로 주입받아 둘을 어떻게 보여 줄지만 정합니다.

여기서 동적 툴 탐색과 메타 툴이 부딪칩니다. `ToolSearchToolCallingAdvisor`는 등록된 툴을 전부 색인 뒤로 숨기고 모델에게 `toolSearchTool` 하나만 보여 줍니다. 그런데 "작업을 위임한다", "스킬을 불러온다" 같은 메타 툴 설명은 사용자의 업무 질의와 의미가 멀어 검색 상위에서 빠지기 쉽습니다. 반대로 모든 툴을 늘 노출하면 라운드마다 툴 정의가 컨텍스트를 차지하고, 작은 로컬 모델일수록 부담이 커집니다.

책의 해법은 툴을 두 레이어로 나누는 것입니다. 메타 툴은 늘 보이게 두고 도메인 툴만 검색으로 찾게 합니다. 이 일을 맡는 `OrchestrationToolCallingAdvisor`는 `ToolSearchToolCallingAdvisor`를 상속해 색인과 검색은 부모에게 맡기고, 부모가 그 라운드에 고른 툴 목록에 메타 툴을 덧붙입니다.

<figure class="wide-figure" markdown>
![강화 에이전트의 에이전트 루프](../assets/figures/fig6-26.png)
<figcaption>강화 에이전트의 에이전트 루프</figcaption>
</figure>

루프의 뼈대는 코어 에이전트와 같습니다. 달라진 점은 메타 툴 상시 노출과 도메인 툴 검색이라는 두 레이어, 그리고 행동 전 명확화 질문과 실행 중 승인이라는 두 가지 사용자 개입입니다.

```java title="OrchestrationToolCallingAdvisor.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java:109:146"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java)</span>

`doBeforeCall`과 `doBeforeStream`은 부모를 먼저 호출해 검색 결과가 반영된 요청을 받은 뒤 메타 툴을 더합니다. 이름이 겹치는 툴은 건너뛰고, 부모가 설정한 다른 옵션은 `mutate()`로 보존하면서 툴 목록만 바꿉니다.

놓치기 쉬운 부분은 시스템 프롬프트 접미사입니다. 부모의 기본 접미사는 필요한 툴이 보이지 않으면 `toolSearchTool`로 찾으라고만 안내합니다. 이대로면 모델이 이미 보이는 메타 툴까지 검색해야 하는 줄 알고 검색을 되풀이할 수 있어서 접미사를 새로 만듭니다.

```java title="OrchestrationToolCallingAdvisor.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java:84:97"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java)</span>

메타 툴마다 이름과 설명 첫 문장을 나열해 검색 없이 바로 부르라고 하고, 도메인 능력이 필요할 때만 `toolSearchTool`을 쓰게 합니다. 결과가 맞지 않으면 표현을 바꿔 다시 검색하라는 지시도 넣습니다.

## 전체 구성과 통합 CLI

<figure class="wide-figure tall" markdown>
![스프링 AI 에이전트 CLI 시스템의 구성](../assets/figures/fig6-27.png)
<figcaption>스프링 AI 에이전트 CLI 시스템의 구성</figcaption>
</figure>

완성한 시스템을 계층으로 펼치면 위와 같습니다. 맨 위 CLI 아래에서 강화 에이전트가 어드바이저 체인으로 루프를 돌리며 전체를 지휘합니다. 로컬 `@Tool`, 커뮤니티 메타 툴, MCP 클라이언트는 하나의 툴 인터페이스로 정리되고, MCP 경계 너머에 운영 서버와 지식 서버, 가장 아래에 모델과 벡터 스토어가 놓입니다.

책의 Step 6 실행 예를 보면 이 부품들이 한 요청 안에서 함께 움직입니다. 세 SKU 중 하나를 사내 재발주 정책대로 점검해 달라는 요청에 에이전트는 먼저 `restock-policy` 스킬을 검색 없이 불러 절차를 확인합니다. 대상 SKU가 정해지지 않았으니 `AskUserQuestionTool`로 선택지를 보여 주며 묻고, 답을 받으면 `toolSearchTool`로 `check_stock`을 찾아 재고를 조회합니다. 안전재고 50개에 모자란 수량을 계산해 `place_purchase_order`를 부르고, 실제 발주 직전에는 승인 게이트가 사람의 확인을 받습니다. 스킬은 절차만 알려 줄 뿐 툴 호출은 메인 에이전트가 직접 한다는 점에서, 일을 격리된 하위 에이전트에 넘기는 `TaskTool`과 실행 위치가 다릅니다.

이 실행 예는 조금 더 큰 `qwen3.5:9b` 모델로 돌린 결과입니다. 4B급 모델은 질문에 답을 받은 뒤 남은 절차를 끝까지 잇지 못하고 멈추는 경우가 많아, 여러 툴을 엮는 시나리오에서는 모델 크기나 추론 수준을 올려 시험하는 편이 좋습니다.

마지막 `Ch6EnhancedSpringAIAgentCli`는 `@Qualifier("enhancedAgent")`로 강화 에이전트를 주입받아 사용자 입력을 같은 `conversationId`로 넘기는 대화 루프입니다. `spring.ai.cli.step`을 주지 않으면 이 통합 CLI가 뜨고, 한 턴이 스트리밍 오류로 실패해도 세션은 이어집니다.

## 4-티어 아키텍처에서의 위치

이 글은 4-티어의 모든 계층을 한 프로젝트에 올립니다. CLI가 채널(T1), `SpringAIAgent`와 `OrchestrationToolCallingAdvisor`가 오케스트레이션(T2), 로컬 툴과 하위 에이전트와 MCP 서버가 능력(T3), 올라마 모델과 벡터 스토어가 파운데이션(T4)에 해당하고, 승인 게이트는 횡단 관심사인 거버넌스를 맡습니다. 새 능력은 MCP 서버나 메타 툴로 더하고 메인 에이전트 코드는 닫아 두는 구조입니다. 남은 횡단 관심사인 관측 가능성은 다음 글 [AI 에이전트 관측 가능성: Micrometer, OTel GenAI 규약, OTLP](../part6/21-observability-for-ai-agents-micrometer-otel-otlp.md)에서 켭니다.

## 이 장의 실습 프로젝트

!!! example "6.6 엔터프라이즈 스프링 AI 에이전트 CLI 프로젝트"
    로컬 툴만 쓰는 코어 에이전트에서 출발해 운영과 지식 MCP 서버, 승인 게이트, 하위 에이전트, 스킬, 메타 툴, 관측 설정을 단계별로 더해 가는 에이전트 CLI입니다. 올라마의 `qwen3.5:4b`와 `bge-m3`가 필요하고, 클라이언트는 연결 대상 서버가 전부 실행 중이어야 시작되므로 서버부터 띄웁니다. 전체 실행 과정은 예제 저장소 [`chapter6/`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)의 README를 따릅니다.

    ```bash
    cd chapter6
    # 터미널 1: 운영 MCP 서버(8085)
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=ops"
    # 터미널 2: 지식 MCP 서버(8086)
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=knowledge"
    # 터미널 3: 통합 CLI(ch6-final), 지식 서버 연결을 인자로 추가
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.mcp.client.streamable-http.connections.knowledge.url=http://localhost:8086"
    ```

## 책에서 더 다루는 내용

!!! book "책 6.5.2~6.5.4, 6.6.3~6.6.6절"
    - MCP 추가 정보 요청과 `AskUserQuestionTool`의 비교, 웹 환경의 `QuestionHandler` 구현 패턴
    - `TodoWriteTool` 등록 코드와 진행 상황 콜백
    - 직접 만든 에이전트 툴과 `TaskTool`의 비교, 빌트인 하위 에이전트 네 가지와 백그라운드 실행
    - 코어 에이전트의 의존성과 설정, 안전 가드 구현, 동적 툴 탐색으로 전환한 실행 결과
    - 운영 서버와 지식 서버의 설정, 단계별 실행 로그
    - `TaskTool`과 `SkillsTool`의 비교, `restock-policy` 스킬과 Step 6 전체 실행 로그

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [Spring AI Agentic Patterns (Part 2): AskUserQuestionTool](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool): spring.io에 실린 `AskUserQuestionTool` 소개 글
- [Spring AI Agentic Patterns (Part 3): TodoWriteTool](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite): spring.io에 실린 `TodoWriteTool` 소개 글
