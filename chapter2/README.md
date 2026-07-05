# chapter2

2장 프로젝트는 기본 채팅을 실제 CLI 챗봇 형태로 확장하는 예제입니다. `ChatClient` 스트리밍, 시스템 프롬프트, 구조화한 출력, 대화 메모리, 어드바이저 체인을 단계별로 확인하고 마지막에는 통합 챗봇을 실행합니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`

```bash
ollama pull qwen3.5:4b
```

## 실행

기본값은 최종 통합 CLI인 `ch2-final`입니다.

```bash
./mvnw spring-boot:run
```

특정 단계를 실행하려면 `spring.ai.cli.step` 값을 지정합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch2-step1"
```

## 단계

| step | 실행 클래스 | 내용 |
| --- | --- | --- |
| `ch2-step1` | `Ch2Step1_BasicStreamChat` | 스트리밍 기반 기본 채팅 |
| `ch2-step2` | `Ch2Step2_PromptTemplate` | 시스템 프롬프트와 역할 부여 |
| `ch2-step3` | `Ch2Step3_StructuredOutput` | `entity(Class)` 기반 구조화한 출력 |
| `ch2-step4` | `Ch2Step4_Memory` | `MessageChatMemoryAdvisor` 기반 대화 메모리 |
| `ch2-step5` | `Ch2Step5_Advisor` | `SimpleLoggerAdvisor`와 커스텀 `ElapsedTimeAdvisor` 체인 |
| `ch2-final` | `Ch2CliChatbotApplication` | 2장 최종 스트리밍 CLI 챗봇 |

## 주요 패키지

| 패키지 | 역할 |
| --- | --- |
| `kr.jmlab.spring.ai.agent.book.chapter2` | 장별 실행 클래스와 최종 CLI |
| `kr.jmlab.spring.ai.agent.book.chapter2.advisor` | 실제 프로젝트에서 사용하는 커스텀 어드바이저 |

## yml 설정

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.ai.cli.step` | `ch2-final` | 기본 실행 러너입니다. `ch2-step1`부터 `ch2-step5`까지 바꿔 실행할 수 있습니다. |
| `spring.ai.model.chat` | `ollama` | Spring AI 2.0 GA 방식의 채팅 모델 제공자 선택입니다. |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | 로컬 Ollama 서버 주소입니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | 스트리밍, 구조화한 출력, 메모리, 어드바이저 예제에서 쓰는 채팅 모델입니다. |
| `logging.level.org.springframework.ai.chat.client.advisor` | `DEBUG` | Step5와 final에서 어드바이저 동작을 확인하기 위한 로그 설정입니다. |

## OpenAI API 사용

OpenAI로 실행하려면 `pom.xml`에 OpenAI starter를 추가하고, Spring AI 2.0 GA 기준 모델 제공자를 OpenAI로 지정합니다.

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

> **모델 선택**: 실습 기본값은 비추론(non-reasoning) 모델 `gpt-4.1-nano`입니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model` 값만 교체하면 됩니다. `gpt-5` 계열도 Spring AI 2.0.0에서 동작하지만 추론 토큰 과금과 응답 지연이 있어, 실습에는 gpt-4 계열(`gpt-4.1-nano`, `gpt-4o-mini`)을 권장합니다.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch2-final"
```

## 확인 포인트

- 구조화한 출력 단계는 완성된 JSON을 객체로 변환해야 하므로 스트리밍이 아니라 `.call()` 흐름을 사용합니다.
- `ch2-final`은 시스템 프롬프트, 메모리, 어드바이저, 스트리밍을 함께 적용합니다.
- 어드바이저 로그는 `application.yml`의 `org.springframework.ai.chat.client.advisor: DEBUG` 설정으로 확인할 수 있습니다.

## 통합 테스트 (IT)

라이브 LLM이 필요한 스텝 실행 테스트는 통합 테스트(`Chapter2StepRunnerIT`)로 분리되어 있습니다. `./mvnw test`는 단위 테스트(`ScriptedConsoleTests`)만 실행하므로 Ollama 없이도 빌드가 멈추지 않고 통과합니다.

### 사전 요구사항

- Ollama 실행 중 (`http://localhost:11434`)
- `qwen3.5:4b` 모델 설치 (`ollama pull qwen3.5:4b`)

### 실행 방법

```bash
# 전체 IT 실행 (스텝별 순차 실행)
./mvnw verify

# 특정 스텝만 실행
./mvnw verify -Dit.test='Chapter2StepRunnerIT#step1_basicStreamChat'
```

### 동작 방식

- 사람이 입력하지 않아도 책의 예시 질문을 여러 턴 자동 입력합니다 (`ScriptedConsole`이 줄 사이 0.4초 간격으로 타이핑을 시뮬레이션).
- 입력 간격은 `-Dchapter2.cli.input-delay-ms=500` 처럼 조절할 수 있습니다.
- 스크립트가 끝나면 `/exit`를 자동 공급하고 테스트당 15분 타임아웃이 있어 입력 대기로 멈추지 않습니다.
- 주의: 여러 모듈의 IT를 동시에 실행하면 Ollama 경합으로 응답이 크게 느려져 타임아웃이 날 수 있습니다. 모듈 하나씩 실행하세요.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 2.60 | 91 | MessageChatMemoryAdvisor 설정 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java) |
| 2.72 | 116 | Step 선택 및 Qwen3.5-4B 모델 지정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 2.74 | 117 | 조건부 활성화 선언(Step 1 예시) | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step1_BasicStreamChat.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step1_BasicStreamChat.java) |
| 2.76 | 119 | 기본 스트림 챗 CLI 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step1_BasicStreamChat.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step1_BasicStreamChat.java) |
| 2.77 | 120 | 페르소나를 주입한 스트림 대화 CLI 구현의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step2_PromptTemplate.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step2_PromptTemplate.java) |
| 2.78 | 122 | call과 entity 매핑을 사용한 대화 CLI 구현의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step3_StructuredOutput.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step3_StructuredOutput.java) |
| 2.79 | 123 | 다중 턴 문맥을 유지하는 대화 CLI 구현의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java) |
| 2.80 | 124 | 응답 시간을 측정하는 커스텀 어드바이저 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/advisor/ElapsedTimeAdvisor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/advisor/ElapsedTimeAdvisor.java) |
| 2.81 | 125 | 다양한 어드바이저를 추가한 어드바이저 체인 구성 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step5_Advisor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step5_Advisor.java) |
| 2.82 | 127 | 최종 CLI 챗봇 구현의 일부 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2CliChatbotApplication.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2CliChatbotApplication.java) |
<!-- book-examples:end -->
