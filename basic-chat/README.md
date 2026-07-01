# basic-chat

2장 도입부에서 사용하는 가장 작은 Spring AI 콘솔 채팅 예제입니다. `ChatClient`를 만들고 Ollama의 로컬 채팅 모델을 한 번 호출하는 흐름을 확인합니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`

필요한 모델이 없다면 먼저 내려받습니다.

```bash
ollama pull qwen3.5:4b
```

## 실행

```bash
./mvnw spring-boot:run
```

애플리케이션은 웹 서버를 띄우지 않는 콘솔형 프로그램입니다. 시작 직후 `안녕 Spring AI!`라는 프롬프트를 보내고 응답을 출력한 뒤 종료합니다. 질문을 바꾸려면 코드의 `userPrompt` 값을 고칩니다.

## 주요 코드

| 파일 | 역할 |
| --- | --- |
| `src/main/java/kr/jmlab/spring/ai/agent/book/SpringAiAgentBookApplication.java` | Spring Boot 엔트리포인트와 `CommandLineRunner` 기반 단발 채팅 실행 |
| `src/main/resources/application.yml` | Ollama 연결 주소와 `qwen3.5:4b` 모델 설정 |

## yml 설정

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.application.name` | `spring-ai-agent-book` | 애플리케이션 이름입니다. |
| `spring.main.web-application-type` | `none` | 웹 서버를 띄우지 않는 콘솔 프로그램으로 실행합니다. |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | 로컬 Ollama 서버 주소입니다. |
| `spring.ai.ollama.chat.think` | `false` | 모델의 추론(thinking)을 끕니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | 기본 채팅 모델입니다. |

Ollama starter 하나만 쓰므로 채팅 모델 제공자 선택(`spring.ai.model.chat`)은 적지 않았습니다. OpenAI starter를 함께 쓸 때만 아래처럼 명시합니다.

## OpenAI API 사용

현재 `pom.xml`은 Ollama starter를 사용합니다. OpenAI로 실행하려면 `spring-ai-starter-model-openai` 의존성을 추가하고, Ollama starter와 동시에 쓸 경우 Spring AI 2.0 GA의 모델 선택 값을 명시합니다.

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
./mvnw spring-boot:run
```

## 확인 포인트

- `ChatClient.Builder`가 자동 주입되는지 확인합니다.
- `.call().content()` 방식의 기본 호출 흐름을 확인합니다.
- 이후 `chapter2`에서 같은 흐름이 스트리밍, 프롬프트, 메모리, 어드바이저로 확장됩니다.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 2.3 | 19 | 빌드 설정 파일 | [`pom.xml`](pom.xml) |
| 2.4 | 21 | 기본 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 2.5 | 22 | 간단한 AI 대화 애플리케이션 | [`src/main/java/kr/jmlab/spring/ai/agent/book/SpringAiAgentBookApplication.java`](src/main/java/kr/jmlab/spring/ai/agent/book/SpringAiAgentBookApplication.java) |
<!-- book-examples:end -->
