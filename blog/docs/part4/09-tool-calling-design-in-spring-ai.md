---
title: "툴 호출 설계: LLM이 현실 세계와 연결되는 방법"
description: "모델은 툴을 고르고 실행은 애플리케이션이 맡습니다. 스프링 AI 툴 호출의 진행 과정, 툴 명세, 툴 컨텍스트와 직접 반환, 결과 변환을 정리합니다."
tags:
  - 4장
---

# 툴 호출 설계: LLM이 현실 세계와 연결되는 방법

<div class="post-meta" markdown>
<span class="tier-chip t3">T3 능력</span> 책 4.1~4.2절 | 예제 [`chapter4`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter4)
</div>

문서 요약이나 초안 작성은 모델의 지식과 RAG로 찾은 문서만 있어도 처리됩니다. 그런데 업무에서 AI에게 맡기고 싶은 일은 오늘 재고 조회, ERP에 결재 건 등록, 고객에게 메일 발송처럼 바깥 시스템을 움직여야 하는 경우가 많습니다. 앞 글의 모듈러 RAG는 답에 필요한 지식을 찾아 주지만 외부 시스템의 상태를 바꾸지는 않습니다. 이 틈을 잇는 기술이 툴 호출입니다.

이 글은 4장의 앞부분인 툴 호출 설계를 다룹니다. 먼저 툴 호출이 어떤 문제에서 출발했는지 오픈AI의 함수 호출 흐름으로 확인하고, 스프링 AI가 그 흐름을 어떤 인터페이스로 나눴는지 봅니다. 이어서 모델에게 툴을 설명하는 명세, 모델에게 보이지 않게 실행 정보를 넘기는 툴 컨텍스트, 툴 결과를 모델에 돌려보내지 않는 직접 반환, 반환값을 문자열로 바꾸는 변환기를 차례로 정리합니다.

## 툴 호출의 등장

LLM이 외부 시스템을 직접 다루지 못하는 이유는 세 가지입니다. 먼저 지식이 학습 시점에 멈춰 있어 그 뒤의 정보를 모릅니다. 또 텍스트를 생성하는 모델이라 API 호출이나 레코드 삽입 같은 코드를 실행하지 못합니다. 서울 날씨를 물어도 계절을 근거로 짐작한 문장을 내놓을 뿐입니다. 마지막으로 기업의 ERP, CRM, 메신저, 결제 시스템은 API 규격과 인증 방식이 제각각이라 그 연동 방법을 모델에 모두 학습시키거나 프롬프트로 일일이 알려 줄 수 없습니다.

초기에는 시스템 프롬프트에 현재 날짜나 검색 결과를 붙여 넣었지만 정보가 늘수록 토큰 비용이 커졌습니다. RAG는 지식 단절을 상당 부분 풀었지만 상태를 바꾸는 작업은 하지 않습니다. API를 호출하는 코드를 짜는 일 자체는 개발자에게 익숙합니다. 어려웠던 것은 모델이 자연어 요청을 해석해 어떤 API를 언제, 어떤 인자로 부를지 스스로 정하고, 그 결과를 다시 답변으로 잇는 흐름을 설계하는 일이었습니다.

이 흐름을 처음 제시한 것이 오픈AI가 2023년 6월에 공개한 함수 호출입니다. 처음에는 `functions` 필드로 함수를 정의했지만, 모델이 코드 인터프리터나 파일 검색까지 다루게 되면서 `tools` 배열로 바뀌었습니다. 함수는 이제 `"type": "function"`으로 정의하는 툴의 한 종류입니다. 핵심은 모델이 함수를 실행하지 않는다는 점입니다. 모델은 어떤 툴을 어떤 인자로 부를지 JSON으로 제안만 하고, 실제 실행은 애플리케이션이 맡습니다.

<figure class="wide-figure" markdown>
![직접 툴 구현 시의 툴 호출 진행 과정](../assets/figures/fig4-1.png)
<figcaption>직접 툴 구현 시의 툴 호출 진행 과정</figcaption>
</figure>

그림은 날씨를 묻는 대화에서 애플리케이션과 모델이 주고받는 과정입니다. 애플리케이션은 사용자 메시지와 함께 `getCurrentWeather` 툴의 이름, 설명, 파라미터 스키마를 `tools` 배열에 담아 모델에 보냅니다(1~2). 모델은 이 툴이 적합하다고 판단하면 메시지에서 `location` 인자를 뽑고, 호출 정보를 `tool_calls`에 넣어 돌려줍니다(3~4). 이때 `finish_reason`이 `"tool_calls"`라면 답변을 끝낸 게 아니라 툴 결과를 기다리며 멈췄다는 뜻입니다. 애플리케이션은 날씨 조회 기능을 실행하고, 결과를 `role: "tool"` 메시지로 대화 기록에 붙여 다시 보냅니다. 어느 호출의 결과인지는 `tool_call_id`로 연결합니다(5~6). 모델이 이 결과로 만든 최종 답변은 `finish_reason`이 `"stop"`인 응답으로 돌아오고, 애플리케이션이 사용자에게 전달합니다(7~8).

툴 호출 이전에는 프롬프트로 출력 형식을 지시하고 정규 표현식으로 응답을 파싱했기 때문에 모델이 형식을 어기면 파싱이 실패했습니다. 툴 호출을 공식 지원하는 모델이 늘면서 JSON을 안정적으로 받게 됐습니다. 이제 프레임워크에는 개발자가 응답 파싱 대신 업무 기능 연동에 집중하게 돕고, 모델마다 다른 툴 호출 규격을 하나로 묶는 역할이 필요해졌습니다.

## 스프링 AI 툴 호출의 특징과 진행 과정

툴이 하는 일은 두 갈래입니다. 데이터베이스, 웹 서비스, 검색 엔진에서 정보를 가져오는 정보 조회와, 메일 발송이나 레코드 생성처럼 시스템에 작업을 일으키는 행동 수행입니다. RAG가 읽기 위주라면 툴 호출은 추론 도중 필요할 때 읽기와 쓰기를 모두 합니다. 툴 호출은 모델의 능력처럼 보이지만 모델에게는 툴을 실행할 권한이 없습니다. 모델은 요청만 하고 실행은 늘 애플리케이션에서 일어나며, 이 경계는 보안 측면에서도 의미가 큽니다.

스프링 AI는 이 흐름을 특정 제공자에 묶이지 않는 추상화로 감쌉니다. 메서드나 함수를 툴로 등록하면 제공자 형식으로의 변환, 툴 호출 요청 감지, 실행, 결과 전달을 프레임워크가 처리합니다.

<figure class="wide-figure" markdown>
![스프링 AI 툴 호출 진행 과정](../assets/figures/fig4-2.jpeg)
<figcaption>스프링 AI 툴 호출 진행 과정 (출처: <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Tool Calling</a>)</figcaption>
</figure>

요청에 툴 정의(이름, 설명, 입력 스키마)를 담아 보내면(1) 모델이 툴 이름과 인자로 응답합니다(2). 애플리케이션은 그 이름으로 툴을 찾아 실행하고(3, 4) 결과를 모델에 돌려주며(5), 모델은 이를 문맥 삼아 최종 응답을 만듭니다(6).

이 과정을 직접 구현한다면 JSON 스키마를 손으로 쓰고, 응답이 텍스트인지 툴 호출인지 매번 분기하고, 인자를 자바 객체로 역직렬화한 뒤 툴 이름과 메서드를 조건문으로 짝지어야 합니다. 결과도 툴 메시지로 직렬화해 대화 기록에 붙이고 모델을 다시 불러야 합니다. 스프링 AI는 이 반복 작업을 세 인터페이스로 나눠 맡깁니다.

- `ToolCallback`: 툴 하나의 명세와 실행을 감쌉니다. 프레임워크가 메서드나 함수 시그니처를 분석해 이 명세를 자동으로 만듭니다.
- `ToolCallingManager`: 툴 실행 생명주기를 맡습니다. 어드바이저 체인에서 툴 호출 응답이 감지되면 인자를 변환해 툴을 실행하고, 결과를 툴 메시지로 대화 기록에 더해 모델을 재호출하게 합니다.
- `ToolCallbackResolver`: 모델이 문자열로 보낸 툴 이름을 실제 `ToolCallback` 객체로 연결합니다. 이름마다 조건문을 둘 필요가 없습니다.

개발자는 비즈니스 로직을 메서드로 작성해 툴로 등록하면 되고, 모델과 주고받는 프로토콜의 세부 구현은 프레임워크에 맡깁니다.

## 툴 명세: 이름, 설명, 스키마

스프링 AI에서는 어떤 방식으로 만든 툴이든 `ToolCallback` 구현체가 됩니다. 이 인터페이스는 모델을 위한 명세와 애플리케이션을 위한 실행을 나눠 둡니다. `getToolDefinition()`은 명세를 돌려주고, `call()`은 모델이 보낸 JSON 입력으로 실제 코드를 실행합니다.

명세인 `ToolDefinition`은 세 요소로 이뤄집니다. 모델에 넘기는 툴 묶음 안에서 겹치면 안 되는 `name`, 모델이 언제 이 툴을 쓸지 판단하는 `description`, 파라미터 구조를 담은 `inputSchema`입니다. 프레임워크는 이 정의를 모아 모델에게 쓸 수 있는 툴을 알리는 프롬프트를 만듭니다. 그래서 명세가 모호하면 모델은 툴을 고르는 단계부터 틀리기 쉽습니다.

```java title="ToolDefinitionExamples.java"
--8<-- "chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/ToolDefinitionExamples.java:20:46"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/ToolDefinitionExamples.java)</span>

`ToolDefinition.builder()`로 세 요소를 직접 정하고, `MethodToolCallback` 빌더에는 명세, 메타데이터, 실행할 메서드와 객체를 따로 넘깁니다. 모델이 보는 정보와 애플리케이션이 실행에 쓰는 정보의 구분이 코드에 그대로 드러납니다. `inputSchema`의 JSON은 요청 전체가 아니라 파라미터 부분의 조각이고, 이 툴은 파라미터가 없어 `properties`가 비어 있습니다. 오픈AI 같은 제공자에 보낼 때는 스프링 AI가 이름과 설명을 위에 두고 이 조각을 `parameters` 아래에 넣어 제공자 형식으로 조립합니다.

다만 파라미터가 바뀔 때마다 JSON 문자열을 고치다 보면 괄호나 쉼표 실수가 잦습니다. 그래서 보통은 `JsonSchemaGenerator`가 메서드 파라미터나 객체 필드를 분석해 스키마를 만들게 합니다. 같은 `current_date` 툴을 애너테이션으로 선언한 코드입니다.

```java title="DateTimeTools.java"
--8<-- "chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/DateTimeTools.java:24:44"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/DateTimeTools.java)</span>

파라미터 설명은 `@ToolParam`으로 붙입니다. 객체를 파라미터로 받는다면 잭슨의 `@JsonClassDescription`, `@JsonPropertyDescription`이나 스웨거의 `@Schema`처럼 이미 쓰던 애너테이션도 인식하고, 중첩된 타입까지 같은 방식으로 스키마를 만듭니다. 아무 표시가 없는 파라미터는 `required`에 들어갑니다. 문맥에 정보가 없는데 필수로 표시된 값은 모델이 지어낼 가능성이 높으므로, `zoneId`처럼 없어도 되는 값은 `required = false`로 밝혀 둡니다. 선택 여부를 정하는 표시가 겹치면 `@ToolParam`, `@JsonProperty`, `@Schema`, `@Nullable` 순으로 우선합니다.

## 툴 컨텍스트와 직접 반환

툴에는 모델이 만든 인자 말고도 로그인한 사용자, 테넌트 ID, 대화 ID처럼 애플리케이션만 아는 값이 필요할 때가 있습니다. 모델은 누가 로그인했는지 모르니 이런 값을 인자로 만들어 줄 수 없습니다. 툴 컨텍스트는 이 값을 요청에 실어 두었다가 툴을 실행하는 시점에만 꺼내 넘깁니다. 컨텍스트는 모델로 전송되지 않으므로 내부 정보를 노출하지 않고 툴 로직에서 쓸 수 있습니다.

직접 반환(`returnDirect`)은 결과가 가는 방향을 바꾸는 설정입니다. 툴 결과는 기본적으로 모델에게 돌아가 최종 답변의 재료가 되지만, 이 속성을 켜면 호출자에게 바로 돌아옵니다. 검색한 원본을 요약 없이 보여 줄 때, 상담원 연결처럼 AI의 개입을 멈추고 제어권을 넘길 때, 다운로드 URL만 전하면 되는 툴처럼 모델의 문장이 필요 없을 때 씁니다.

<figure class="wide-figure" markdown>
![직접 반환 설정 시 툴 호출 진행 과정](../assets/figures/fig4-4.jpeg)
<figcaption>직접 반환 설정 시 툴 호출 진행 과정 (출처: <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Tool Calling</a>)</figcaption>
</figure>

그림에서 툴 실행 결과는 모델을 다시 거치지 않고 곧바로 응답이 됩니다. 이 분기는 `ToolCallingManager`가 툴의 `returnDirect` 값을 보고 정합니다. 모델이 여러 툴을 한꺼번에 요청했다면 모든 툴이 직접 반환일 때만 이렇게 끝나고, 하나라도 아니면 결과를 모두 모델에 보냅니다.

```java title="Chapter4ToolCallbacks.java"
--8<-- "chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/Chapter4ToolCallbacks.java:64:85"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/Chapter4ToolCallbacks.java)</span>

세션 요약 툴은 `BiFunction`의 두 번째 인자로 `ToolContext`를 받아 사용자 이름과 대화 ID를 꺼냅니다. 모델이 만든 입력에는 `topic`만 있습니다. Step 3 실행기 `Ch4Step3_ToolContext`는 이 툴을 `sessionSummary(true)`로 등록하고, 요청마다 `.toolContext(Map.of(...))`로 두 값을 넘깁니다. 직접 반환을 켰으므로 책의 실행 결과에서도 툴이 만든 요약문이 모델의 후속 답변 없이 그대로 출력됩니다.

이 코드에서 두 설정의 성격 차이도 보입니다. `ToolContext`는 요청마다 달라지는 실행 시점의 데이터이고, `returnDirect`를 담은 `ToolMetadata`는 툴을 등록할 때 정해지는 고정 속성입니다. 직접 반환은 응답을 조금 빠르게 하는 옵션이라기보다 추론 루프를 어디서 끊을지 정하는 실행 전략입니다. 토큰 비용, 지연 시간, 모델에 노출되는 범위를 이 설정으로 조절합니다.

## 툴 호출 결과 변환

대부분의 모델 API는 툴 결과를 문자열로 받습니다. 스프링 AI에서는 함수형 인터페이스 `ToolCallResultConverter`가 툴이 반환한 객체와 반환 타입을 받아 문자열로 바꿉니다. 따로 지정하지 않으면 `DefaultToolCallResultConverter`가 잭슨으로 JSON 직렬화를 합니다. 반환 타입이 `void`면 `"Done"`을, 결과가 `null`이면 `"null"`을 보내 정상 완료와 값 없음을 구분합니다.

기본 JSON으로 부족할 때는 변환기를 직접 구현합니다. 민감 정보를 가리거나 XML, CSV 같은 형식이 필요한 경우입니다.

```java title="EmailMaskingToolCallResultConverter.java"
--8<-- "chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/EmailMaskingToolCallResultConverter.java:12:22"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter4/src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/EmailMaskingToolCallResultConverter.java)</span>

직렬화는 `delegate`로 둔 `DefaultToolCallResultConverter`에 맡기고, 결과 문자열에서 이메일 주소만 `[EMAIL]`로 치환합니다. `Chapter4ToolCallbacks`의 고객 연락처 조회 툴은 빌더의 `toolCallResultConverter()`로 이 변환기를 지정하므로 모델은 가려진 결과만 봅니다. 단위 테스트 `Chapter4ToolTests`도 결과에 원래 주소가 남지 않는지 확인합니다.

## 4-티어 아키텍처에서의 위치

툴은 4-티어 아키텍처에서 에이전트가 쓰는 외부 능력, 곧 T3 능력 계층의 기본 단위입니다. 명세와 실행이 `ToolCallback` 하나로 묶이기 때문에 메서드든 함수든 툴을 고르고 루프를 돌리는 T2 오케스트레이션 계층에는 같은 툴로 보입니다. 모델은 명세만 보고 툴을 고르고, 실행 권한과 툴 컨텍스트 같은 내부 정보는 애플리케이션에 남습니다. 메서드와 함수를 툴로 구현하는 방법, 그리고 툴 루프의 실행 제어권을 프레임워크, 어드바이저, 사용자 코드 중 어디에 둘지는 다음 글 [툴 구현과 실행 제어: @Tool에서 ToolCallingManager까지](../part4/10-tool-implementation-and-execution-control.md)에서 다룹니다.

## 책에서 더 다루는 내용

!!! book "책 4.1~4.2절"
    - 오픈AI 함수 호출 명세로 따라가는 요청과 응답 JSON 전체, 병렬 툴 호출 옵션
    - `ToolCallback`, `ToolDefinition` 인터페이스 소스와 `ToolContext`를 받는 `call()`의 하위 호환 설계
    - 스프링 AI가 오픈AI로 보내는 실제 툴 JSON 페이로드
    - 스키마 설명을 만드는 애너테이션별 제공 라이브러리와 용도 정리
    - `ToolCallingChatOptions`로 툴 컨텍스트를 넣을 때 기본 설정과 병합되는 규칙
    - `RenderedImage`를 포함한 반환 타입별 `DefaultToolCallResultConverter` 처리 방식

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [OpenAI Function calling](https://developers.openai.com/api/docs/guides/function-calling/): 오픈AI가 설명하는 함수 호출과 툴 호출
- [스프링 AI의 챗 모델 비교](https://docs.spring.io/spring-ai/reference/api/chat/comparison.html): 제공자별 툴 호출 등 기능 지원 여부를 모은 표
- [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html): 스프링 AI 툴 호출 레퍼런스 문서
