---
title: "스프링 AI 에이전트 아키텍처와 구현, 그리고 동적 툴 탐색"
description: "단일 루프와 툴 실행 영역으로 이루어진 스프링 AI 에이전트 아키텍처를 구현하고, 툴을 찾아 주는 툴로 능력을 동적으로 넓히는 동적 툴 탐색을 다룹니다."
tags:
  - 6장
---

# 스프링 AI 에이전트 아키텍처와 구현, 그리고 동적 툴 탐색

<div class="post-meta" markdown>
<span class="tier-chip t2">T2 오케스트레이션</span><span class="tier-chip t3">T3 능력</span> 책 6.4.3절, 6.4.5절 | 예제 [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

앞선 글들에서는 에이전트가 실행 루프로 움직인다는 것, 루프마다 모델에 넘길 컨텍스트를 설계하는 컨텍스트 엔지니어링, 그리고 [재귀적 어드바이저로 툴 루프를 제어하는 방법](../part6/16-recursive-advisor-and-tool-loop-control.md)을 살펴봤습니다. 이번 글에서는 이 부품들을 조합해 실제로 동작하는 에이전트를 만듭니다.

설계를 처음부터 새로 할 필요는 없습니다. 클로드 코드처럼 널리 쓰이는 에이전트 제품은 내부 구조를 문서와 오픈소스로 공개하고 있습니다. 책은 이런 제품들의 공통 구조를 스프링 AI 모듈에 대응시킨 '스프링 AI 에이전트 아키텍처'를 제시하고, 예제 저장소는 이를 `SpringAIAgent` 클래스로 구현합니다.

글의 뒷부분은 툴이 많아졌을 때 생기는 문제를 다룹니다. 등록한 툴 정의를 매번 전부 모델에 보내는 대신, 필요한 툴만 검색해 넘기는 동적 툴 탐색입니다.

## 스프링 AI 에이전트 아키텍처

시장을 이끄는 에이전트 제품들에는 세 가지 공통점이 있습니다. 에이전트 루프가 하나이고, 모든 능력이 하나의 툴 인터페이스로 이어지며, 외부 연결은 MCP로 표준화되어 있습니다. 스프링 AI에는 이 구조에 맞는 부품이 이미 갖춰져 있습니다. 새 프레임워크를 익히기보다는, 지금까지 배운 부품을 에이전트의 관점에서 다시 배치하는 작업에 가깝습니다.

- **루트 에이전트**: `ChatClient`와 어드바이저 체인이 중심을 잡고, 재귀적 어드바이저인 `ToolCallingAdvisor`가 툴 루프를 제어합니다. 모델 호출은 `ChatModel` 추상화가 맡으므로 특정 벤더에 묶이지 않습니다.
- **컨텍스트와 메모리**: 페르소나와 행동 지침은 시스템 프롬프트에 담고, 대화 기록은 `MessageChatMemoryAdvisor`로 관리합니다.
- **툴 실행 영역**: `@Tool`과 `ToolCallback`이 단일 툴 인터페이스입니다. 간단한 조회든 복잡한 업무 로직이든 에이전트에는 같은 모양으로 등록됩니다.
- **외부 확장**: MCP 클라이언트가 외부 서버의 툴을 모델에 보여 줍니다. 외부 에이전트를 연결하는 통로이고, 툴 실행 중에 사람의 승인을 받는 채널도 MCP의 추가 정보 요청으로 동작합니다.
- **에이전트 전문 툴**: 계획, 질문, 스킬, 위임을 맡는 툴은 커뮤니티 라이브러리인 `spring-ai-agent-utils`에서 가져옵니다. 클로드 코드에서 영감을 받은 구현 패턴을 스프링 AI로 옮긴 라이브러리입니다.

<figure class="wide-figure" markdown>
![스프링 AI 에이전트 아키텍처](../assets/figures/fig6-15.png)
<figcaption>스프링 AI 에이전트 아키텍처</figcaption>
</figure>

그림의 구성 요소는 출처에 따라 둘로 나뉩니다. 루프 제어와 툴 인터페이스, MCP 통신은 스프링 AI 코어에 있습니다. 사람의 승인을 받는 추가 정보 요청(`@McpElicitation`)과 툴이 많을 때 쓰는 `ToolSearchToolCallingAdvisor`도 코어에 속합니다. 반면 `TodoWriteTool`(작업 계획), `AskUserQuestionTool`(명확화 질문), `SkillsTool`(스킬), `TaskTool`(하위 에이전트 위임)은 커뮤니티가 이끄는 인큐베이팅 라이브러리에 들어 있습니다.

## SpringAIAgent 구현: ChatClient와 어드바이저 체인

코어 부분만으로도 싱글 에이전트를 만들 수 있습니다. `ChatClient`, `ToolCallingAdvisor`, `@Tool`, MCP 클라이언트, 대화 메모리는 모두 2장부터 5장까지 써 온 기본 스타터에 들어 있어서 라이브러리를 더 추가하지 않아도 됩니다. 툴도 새로 만들지 않고 4장에서 만든 `DateTimeTools`, `CalculatorTools`, `InventoryTools`를 가져와 씁니다.

예제 저장소는 이 코어 에이전트와, 뒤에서 커뮤니티 툴을 더해 만들 강화 에이전트를 모두 `SpringAIAgent` 클래스 하나로 만듭니다. 둘이 다른 부분은 시스템 프롬프트, 툴 목록, 툴 루프 어드바이저뿐이라서 이 셋을 생성자로 받습니다.

```java title="SpringAIAgent.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java:19:52"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java)</span>

코드 어디에도 `while`이나 `for` 반복문이 없습니다. 모델이 툴 호출을 요청하면 `ToolCallingAdvisor`가 어드바이저 체인 안에서 툴을 실행하고, 결과를 대화에 넣은 뒤 모델을 다시 부릅니다. 모델이 최종 답을 낼 때까지 이 과정이 되풀이됩니다. 스프링 AI 2.0은 툴 루프를 내부에서 자동으로 수행합니다. 그래도 예제는 루프 어드바이저를 직접 등록합니다. 메모리 어드바이저와의 실행 순서를 코드에 드러내고, 툴 실행 라운드 수를 제한하는 안전 가드를 함께 설정하기 위해서입니다. 루프 어드바이저는 `Supplier`로 받아 요청마다 새로 만들기 때문에, 이 가드의 카운터가 요청끼리 섞이지 않습니다.

순서값을 보면 구조가 드러납니다. `MessageChatMemoryAdvisor`는 `HIGHEST_PRECEDENCE + 200`으로 루프 바깥에 있어 요청당 한 번 실행되고, `run()`에서 붙이는 루프 어드바이저는 `+300`으로 그 안쪽에 자리합니다. `ThinkTraceAdvisor`와 `ToolCallTraceAdvisor`는 루프 안에서 반복마다 모델의 생각과 호출한 툴을 콘솔에 보여 줍니다. 툴 호출 기록까지 저장소에 남기고 싶다면 메모리 어드바이저를 루프 안쪽으로 옮기고 `ToolCallingAdvisor`의 내부 대화 기록을 끄도록 구성할 수도 있습니다. 그러면 기록 관리는 메모리 어드바이저 한곳이 맡습니다.

채널 쪽 코드는 `run()`에 사용자 메시지와 대화 ID만 넘기면 됩니다.

```java title="Ch6Step1_SpringAIAgent.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/Ch6Step1_SpringAIAgent.java:24:41"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/Ch6Step1_SpringAIAgent.java)</span>

Step 1 러너는 `coreAgent` 빈을 주입받아 같은 대화 ID로 두 번 묻습니다. 첫 요청은 현재 시간 조회, 재고 확인, 예약을 한꺼번에 시키고, 두 번째 요청은 방금 예약한 상품 이름을 물어 대화 메모리가 동작하는지 확인합니다. MCP 서버 없이 돌리려면 `--spring.ai.cli.step=ch6-step1 --spring.ai.mcp.client.enabled=false` 인자로 실행합니다.

## 단일 툴 인터페이스: 로컬, 커뮤니티, 원격

능력이 어디에서 오든 에이전트에 들어갈 때는 `ToolCallback`이 됩니다. 이 조립은 `AgentConfig`의 `coreAgent` 빈이 맡습니다.

```java title="AgentConfig.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java:70:99"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java)</span>

로컬 툴은 `localTools` 빈이 세 툴 객체를 `ToolCallbacks.from()`으로 변환해 만든 `List<ToolCallback>`입니다. MCP 서버의 툴은 `SyncMcpToolCallbackProvider`가 제공하는데, `getToolCallbacks()`로 펼치면 로컬 툴과 같은 목록에 합쳐집니다. MCP 클라이언트를 꺼서 이 빈이 없으면 `getIfAvailable()`이 `null`을 돌려주고, 에이전트는 로컬 툴만으로 만들어집니다. JVM 안의 메서드든 네트워크 너머의 서버든 모델이 보기에는 똑같은 툴 호출입니다.

커뮤니티 툴도 같은 길로 들어옵니다. `SkillsTool`과 `TaskTool`은 빌더가 곧바로 `ToolCallback`을 만들고, `TodoWriteTool`과 `AskUserQuestionTool`은 `@Tool` 객체라서 `ToolCallbacks.from()`으로 바꿔 합칩니다. 능력을 더해도 `SpringAIAgent`는 그대로이고, 설정 클래스가 넘기는 목록만 달라집니다.

## 동적 툴 탐색: 툴을 찾아 주는 툴

툴을 늘리는 데는 대가가 따릅니다. 등록한 툴의 이름, 설명, JSON 스키마는 요청마다 모델에 전달되어 컨텍스트를 차지합니다. 책이 인용한 자료에 따르면 여러 MCP 서버를 연결한 흔한 구성에서는 본격적인 대화 전에 50개가 넘는 툴의 정의만으로 55,000토큰 넘게 쓸 수 있습니다. 이름과 기능이 비슷한 툴이 늘어날수록 모델이 맞는 툴을 고르는 정확도도 눈에 띄게 떨어집니다.

툴 탐색(tool search) 패턴은 이 문제를 검색으로 풉니다. 처음에는 툴을 찾아 주는 툴(Tool Search Tool, TST) 하나만 모델에 보여 줍니다. 작업 중에 어떤 능력이 필요해지면 모델이 이 툴에 검색어를 넘기고, 검색에 걸린 툴의 정의만 컨텍스트에 추가됩니다. 툴을 관리하는 일까지 툴 호출로 풀었다는 점에서 '모든 능력은 툴로 연결된다'는 원칙을 그대로 따릅니다.

이 패턴은 앤트로픽이 클로드 전용 기능으로 먼저 제시했고, 스프링 AI는 같은 아이디어를 재귀적 어드바이저로 풀었습니다. 구현체는 `ToolCallingAdvisor`의 하위 클래스인 `ToolSearchToolCallingAdvisor`입니다. 툴 루프를 프레임워크 수준에서 가로채므로 오픈AI, 앤트로픽, 제미나이, 올라마 어느 모델에서나 같은 방식으로 동작합니다.

<figure class="wide-figure" markdown>
![툴을 찾아주는 툴](../assets/figures/fig6-17.png)
<figcaption>툴을 찾아주는 툴 (출처: <a href="https://spring.io/blog/2025/12/11/spring-ai-tool-search-tools-tzolov">Smart Tool Selection: Achieving 34-64% Token Savings with Spring AI’s Dynamic Tool Discovery</a>)</figcaption>
</figure>

그림의 번호를 따라가면 흐름은 일곱 단계입니다.

1. 애플리케이션이 시작할 때 등록된 툴을 모두 `ToolIndex`에 색인합니다.
2. 첫 요청에는 전체 툴 대신 검색 툴 정의만 담아 보냅니다.
3. 능력이 필요하다고 판단한 모델이 검색어를 넣어 검색 툴을 호출합니다.
4. `ToolIndex`가 검색어에 맞는 툴을 찾고, 그 정의를 다음 요청의 컨텍스트에 더합니다.
5. 모델이 새로 보이게 된 실제 툴을 호출합니다.
6. 애플리케이션이 그 툴을 실행하고 결과를 모델에 돌려줍니다.
7. 필요한 정보를 모은 모델이 사용자에게 최종 답을 냅니다.

검색 방식은 `ToolIndex` 인터페이스로 분리되어 있습니다. 키워드 기반 `LuceneToolIndex`, 임베딩으로 의미를 비교하는 `VectorToolIndex`, 정규식 기반 `RegexToolIndex` 중에서 고르거나 직접 구현해 넣을 수 있습니다. 의존성은 `spring-ai-starter-tool-search-advisor`입니다. 이 어드바이저는 검색으로 찾은 툴 정의를 세션 단위로 쌓아 두기 때문에 호출할 때 대화 ID가 필요합니다. `SpringAIAgent.run()`은 요청마다 이 값을 넘기므로 따로 챙길 것이 없습니다.

예제의 코어 에이전트는 툴 수를 보고 두 어드바이저 가운데 하나를 고릅니다. 앞의 `coreAgent` 코드에서 로컬 툴과 원격 툴을 더한 수가 `spring.ai.cli.tool-search.min-tools`(설정이 없으면 10) 이상이고 벡터 스토어가 있으면 동적 툴 탐색을 켭니다. 어드바이저를 만드는 부분은 다음과 같습니다.

```java title="AgentConfig.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java:174:192"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java)</span>

동적 툴 탐색을 켜면 `VectorToolIndex`는 한 번만 만들고, 요청마다 새로 만드는 `ToolSearchToolCallingAdvisor`가 그 색인을 같이 씁니다. 색인은 세션별 임베딩을 추적해 정리하는데, 요청마다 새로 만들면 벡터 스토어에 중복이 쌓입니다. `maxResults`에는 한 번의 검색으로 활성화할 툴 수의 상한인 5를 줍니다. 색인이 쓰는 저장소는 `toolVectorStore` 빈의 `SimpleVectorStore`이고, 임베딩 모델은 `application.yml`의 `spring.ai.ollama.embedding.model`에 지정한 `bge-m3`입니다.

툴 수를 세어 보면 언제 전환되는지 알 수 있습니다. 로컬 툴은 날짜 2개, 계산 2개, 재고 3개로 모두 7개입니다. 운영 MCP 서버의 툴 2개를 더해도 9개라 전체 툴을 그대로 싣고, 툴이 10개 이상이 되어야 검색으로 넘어갑니다. Step 4부터 쓰는 강화 에이전트는 이 규칙 대신, 메타 툴은 항상 보여 주고 도메인 툴만 검색하는 `OrchestrationToolCallingAdvisor`를 씁니다. 이 구성은 [엔터프라이즈 에이전트 CLI 글](../part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md)에서 다룹니다.

도입 효과는 공개된 예비 측정 결과로 가늠할 수 있습니다. 관련 툴 3개와 무관한 툴 25개를 섞어 28개로 같은 과제를 풀게 한 측정에서, 루씬 검색을 적용하자 총 토큰 사용량이 제미나이는 60%, 오픈AI는 34%, 앤트로픽은 64% 줄었습니다. 절감분은 대부분 요청마다 실리던 툴 정의에서 나오고, 검색 단계가 끼어드는 만큼 LLM 호출은 평균 1~2회 늘었습니다. 벡터 검색을 썼을 때도 47~63%로 비슷하게 줄었습니다. 문서를 검색해 넣는 RAG처럼 툴 정의를 검색해 필요한 능력만 공급한다는 점에서, 이 패턴을 '툴을 위한 RAG'로 볼 수 있습니다.

## 4-티어 아키텍처에서의 위치

`SpringAIAgent`와 어드바이저 체인은 T2 오케스트레이션입니다. 이 계층이 루프를 돌리면서 T3 능력 계층의 툴을 부르고, 모델 호출은 T4 파운데이션의 `ChatModel`에 맡깁니다. 예제의 패키지 이름도 `channel`, `orchestration`, `capability`로 티어를 따릅니다. 동적 툴 탐색은 T2와 T3의 경계에서 일합니다. 라운드마다 어떤 능력을 모델에 보여 줄지를 오케스트레이션 계층이 검색으로 정하기 때문입니다. 이제 에이전트가 원격 툴까지 호출하게 되었으니, 다음 글에서는 되돌릴 수 없는 작업 앞에서 사람의 승인을 받는 방법을 봅니다: [사용자 개입(HITL): MCP Elicitation으로 만드는 승인 게이트](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)

## 책에서 더 다루는 내용

!!! book "책 6.4.3절, 6.4.5절"
    - 표 6.12 스프링 AI 에이전트 개발 스택: 구성 요소별 역할, 코어와 커뮤니티 구분, 모듈 아티팩트
    - 예제 6.17 스프링 AI 코어만으로 구현한 기본 에이전트: 메모리 어드바이저를 루프 안쪽에 두고 내부 대화 기록을 끈 구성
    - `LuceneToolIndex`, `VectorToolIndex`, `RegexToolIndex` 가운데 하나를 골라 `ToolSearchToolCallingAdvisor`에 넣는 등록 예제
    - 표 6.13 모델별 Tool Search 적용 전후의 토큰 사용량 수치

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [Smart Tool Selection: Achieving 34-64% Token Savings with Spring AI’s Dynamic Tool Discovery](https://spring.io/blog/2025/12/11/spring-ai-tool-search-tools-tzolov): `ToolSearchToolCallingAdvisor`의 동작 흐름과 모델별 토큰 절감 측정 결과
- [Introducing advanced tool use on the Claude Developer Platform](http://www.anthropic.com/engineering/advanced-tool-use): 앤트로픽이 클로드 전용 기능으로 제시한 동적 툴 탐색
