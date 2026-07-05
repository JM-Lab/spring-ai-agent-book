# chapter4

4장 프로젝트는 Spring AI Tool Calling을 CLI 챗봇에 붙이는 예제입니다. `@Tool` 메서드, 함수형 툴, `ToolContext`, `ToolCallingManager`, `ToolCallingAdvisor`의 차이를 단계별로 확인합니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`

```bash
ollama pull qwen3.5:4b
```

## 실행

기본값은 최종 Tool 지원 CLI인 `ch4-final`입니다.

```bash
./mvnw spring-boot:run
```

특정 단계를 실행하려면 `spring.ai.cli.step` 값을 지정합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch4-step5"
```

## 단계

| step | 실행 클래스 | 내용 |
| --- | --- | --- |
| `ch4-step1` | `Ch4Step1_MethodTools` | `@Tool` 메서드 기반 툴 등록 |
| `ch4-step2` | `Ch4Step2_FunctionTools` | `FunctionToolCallback` 기반 함수형 툴 |
| `ch4-step3` | `Ch4Step3_ToolContext` | `ToolContext`와 `returnDirect` 메타데이터 |
| `ch4-step4` | `Ch4Step4_ToolCallingManager` | 툴 실행 루프 직접 제어 |
| `ch4-step5` | `Ch4Step5_ToolCallingAdvisor` | ChatClient 어드바이저 체인에 툴 호출 통합 |
| `ch4-final` | `Ch4ToolCliChatbotApplication` | 4장 최종 Tool 지원 CLI 챗봇 |

## 주요 코드

| 파일 | 역할 |
| --- | --- |
| `Ch4Step1_MethodTools.java` | 가장 기본적인 메서드 툴 등록 흐름 |
| `Ch4Step4_ToolCallingManager.java` | 모델 응답과 툴 실행을 애플리케이션이 직접 조율하는 흐름 |
| `Ch4Step5_ToolCallingAdvisor.java` | 툴 호출을 ChatClient 호출 흐름에 자연스럽게 통합하는 방식 |

## yml 설정

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.ai.cli.step` | `ch4-final` | 기본 실행 러너입니다. `ch4-step1`부터 `ch4-step5`까지 바꿔 실행할 수 있습니다. |
| `spring.ai.model.chat` | `ollama` | Spring AI 2.0 GA 방식의 채팅 모델 제공자 선택입니다. |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | 로컬 Ollama 서버 주소입니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | Tool Calling 실습에 쓰는 채팅 모델입니다. |

## OpenAI API 사용

4장은 Tool Calling이 핵심이므로 OpenAI 모델도 툴 호출을 지원하는 모델을 사용해야 합니다. OpenAI로 실행하려면 OpenAI starter를 추가하고 모델 제공자를 OpenAI로 지정합니다.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4.1-nano
```

> **모델 선택**: 실습 기본값은 비추론(non-reasoning) 모델 `gpt-4.1-nano`입니다. 이 모델은 툴 호출을 지원하므로 4장 예제가 정상 동작합니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model` 값만 교체하면 됩니다. `gpt-5` 계열도 Spring AI 2.0.0에서 동작하지만 추론 토큰 과금과 응답 지연이 있어, 실습에는 gpt-4 계열(`gpt-4.1-nano`, `gpt-4o-mini`)을 권장합니다.

> **OpenAI 전환 시 4장 주의(실측)**: `.call()` 기반 툴 예제(`ch4-step1`~`step3`, `step5`)와 `ch4-final`의 툴 자동 호출 자체는 OpenAI에서 정상 동작합니다. 다만 `ch4-final`은 스트리밍(`.stream()`)과 툴 호출을 함께 쓰기 때문에, OpenAI 제공자에서는 `400 (messages with role 'tool' must be a response to a preceeding message with 'tool_calls')` 오류가 납니다. 스트리밍 통합 CLI는 Ollama로 실행하고, OpenAI에서는 비스트리밍 단계 예제로 확인하세요.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch4-final"
```

### `ch4-step4`를 OpenAI로 실행하기

`ch4-step4`의 `ManualToolCallingService`는 `ChatClient`를 거치지 않고 저수준 `ChatModel.call(prompt)`을 직접 호출합니다. 이 경로에서는 **런타임 옵션이 제공자별 옵션 타입이어야 합니다.** `OpenAiChatModel`은 `buildRequestPrompt`에서 옵션이 이미 지정돼 있으면 그대로 통과시키고, `createRequest`에서 `(OpenAiChatOptions)`로 하드캐스팅합니다. 따라서 `OllamaChatOptions`(또는 제공자 중립 `ToolCallingChatOptions`, `DefaultChatOptions`)를 넘기면 `ClassCastException`이 발생합니다. 포터블 옵션 변환은 `ChatClient` 계층에서만 일어나기 때문이며, `ChatClient`를 쓰는 다른 단계(`ch4-step1`~`step3`, `step5`)가 OpenAI에서 문제없이 도는 이유도 같습니다.

이 예제를 OpenAI로 실행하려면 옵션 생성 부분만 `OpenAiChatOptions`로 바꿉니다.

```java
// import org.springframework.ai.openai.OpenAiChatOptions;

// 기본값은 OllamaChatOptions. OpenAI로 실행할 때만 아래처럼 교체한다.
var options = OpenAiChatOptions.builder()
        .toolCallbacks(this.toolCallbacks)
        .temperature(0.1)
        .build();
```

> `gpt-5` 계열은 `temperature`를 기본값(1)만 허용하므로 위 `temperature(0.1)`에서 거부됩니다. 이 예제는 `gpt-4.1-nano` 등 gpt-4 계열로 실행하세요.

## 확인 포인트

- 모델이 툴 호출을 지원해야 단계별 예제가 의도대로 동작합니다.
- `ToolCallingManager`는 실행 제어를 코드가 직접 쥐는 방식이고, `ToolCallingAdvisor`는 ChatClient 체인 안에 툴 호출을 통합하는 방식입니다.
- `ch4-final`은 사용자의 자연어 요청에 따라 등록된 툴을 자동 호출하는 CLI 챗봇입니다.

## 통합 테스트 (IT)

라이브 LLM이 필요한 스텝 실행 테스트는 통합 테스트(`Chapter4StepRunnerIT`)로 분리되어 있습니다. `./mvnw test`는 Ollama가 필요 없는 단위 테스트(`Chapter4ToolTests`)만 실행합니다.

### 사전 요구사항

- Ollama 실행 중 (`http://localhost:11434`)
- `qwen3.5:4b` 모델 설치 (툴 호출 지원 모델)

### 실행 방법

```bash
# 전체 IT 실행
./mvnw verify

# 특정 스텝만 실행
./mvnw verify -Dit.test='Chapter4StepRunnerIT#step1_methodTools'
```

### 동작 방식

- 책의 예시 질문(재고 조회, 할인 계산, 예약 등)을 여러 턴 자동 입력합니다 (`CliInputSimulator`, 줄 사이 기본 0.4초).
- 입력 간격은 `-Dchapter4.cli.input.delay-ms=500` 으로 조절할 수 있습니다.
- 스크립트 종료 후 `/exit` 자동 공급, 테스트당 15분 타임아웃.
- 주의: 여러 모듈의 IT를 동시에 실행하면 Ollama 경합으로 타임아웃이 날 수 있습니다. 모듈 하나씩 실행하세요.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 4.12 | 293 | 커스텀 ToolCallResultConverter 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/EmailMaskingToolCallResultConverter.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/EmailMaskingToolCallResultConverter.java) |
| 4.31 | 322 | 4장 주요 의존성 | [`pom.xml`](pom.xml) |
| 4.32 | 322 | 4장 프로젝트 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 4.33 | 323 | @Tool 메서드 기반 툴 사용 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step1_MethodTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step1_MethodTools.java) |
| 4.34 | 324 | FunctionToolCallback 기반 툴을 사용한 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step2_FunctionTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step2_FunctionTools.java) |
| 4.35 | 325 | ToolContext와 returnDirect를 사용한 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step3_ToolContext.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step3_ToolContext.java) |
| 4.36 | 326 | ToolCallingManager로 툴 실행을 제어하는 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/ManualToolCallingService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/examples/ManualToolCallingService.java) |
| 4.37 | 327 | ToolCallingAdvisor로 툴 실행을 자동화하는 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step5_ToolCallingAdvisor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/Ch4Step5_ToolCallingAdvisor.java) |
| 4.38 | 328 | 툴 지원 AI 챗봇 CLI에서 사용하는 상품 재고 툴의 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/InventoryTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/InventoryTools.java) |
| 4.39 | 329 | 툴 지원 AI 챗봇 CLI에서 사용하는 할 일 관리 툴의 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/TodoTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/TodoTools.java) |
| 4.40 | 330 | 툴 지원 AI 챗봇 CLI의 주요 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/ToolEnabledChatService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter4/support/ToolEnabledChatService.java) |
<!-- book-examples:end -->
