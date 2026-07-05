# chapter5

5장 프로젝트는 3장의 RAG 기능을 MCP 서버로 노출하고, 별도 CLI 클라이언트가 MCP 서버의 툴, 리소스, 프롬프트를 사용하는 예제입니다. 하나의 프로젝트 안에 `server` 프로파일과 기본 클라이언트 실행 흐름이 함께 들어 있습니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`
- 임베딩 모델: `bge-m3`

```bash
ollama pull qwen3.5:4b
ollama pull bge-m3
```

## 서버 실행

MCP 서버는 `server` 프로파일로 실행하며 기본 포트는 `8085`, MCP 엔드포인트는 `/mcp`입니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server --spring.ai.cli.step=ch5-server-step4"
```

서버 단계만 따로 확인할 수도 있습니다.

| step | 실행 클래스 | 내용 |
| --- | --- | --- |
| `ch5-server-step1` | `Ch5ServerStep1_RagKnowledgeBase` | RAG 문서 적재와 벡터 검색 기반 지식 베이스 확인 |
| `ch5-server-step2` | `Ch5ServerStep2_SearchTools` | `rag_index_summary`, `rag_search_documents` 검색 툴 제공 |
| `ch5-server-step3` | `Ch5ServerStep3_AnswerTools` | `rag_answer_question` 답변 툴 제공 |
| `ch5-server-step4` | `Ch5ServerStep4_McpServerPrimitives` | MCP 리소스, 프롬프트, 자동완성 프리미티브 제공 |

## 클라이언트 실행

다른 터미널에서 서버를 먼저 실행한 뒤 클라이언트를 실행합니다. 기본값은 최종 MCP CLI인 `ch5-final`입니다.

```bash
./mvnw spring-boot:run
```

특정 클라이언트 단계를 실행하려면 `spring.ai.cli.step` 값을 지정합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch5-client-step3"
```

| step | 실행 클래스 | 내용 |
| --- | --- | --- |
| `ch5-client-step1` | `Ch5Step1_McpDiscovery` | MCP 서버 초기화와 기능 발견 |
| `ch5-client-step2` | `Ch5Step2_McpPrimitiveCalls` | 리소스, 프롬프트, 자동완성 직접 호출 |
| `ch5-client-step3` | `Ch5Step3_ToolCallbackProvider` | MCP 툴을 ChatClient 툴로 연결 |
| `ch5-client-step4` | `Ch5Step4_McpClientPolicy` | 클라이언트 측 MCP 사용 정책 적용 |
| `ch5-final` | `Ch5McpCliChatbotApplication` | 5장 최종 MCP 연동 CLI 챗봇 |

## 주요 설정

| 파일 | 역할 |
| --- | --- |
| `src/main/resources/application.yml` | 두 문서로 나뉜 설정 파일. 첫 문서는 클라이언트 설정(`http://localhost:8085/mcp` 연결), `---` 뒤 두 번째 문서는 `server` 프로파일에서만 적용되는 MCP 서버 설정(포트, 서버 이름과 프로토콜, 엔드포인트). 툴, 리소스, 프롬프트, 자동완성 공개와 변경 알림은 기본값(`true`)을 그대로 씁니다 |
| `src/main/resources/data` | 서버가 RAG 지식 베이스로 읽는 문서 |

## yml 설정

### 클라이언트 설정 (application.yml 첫 문서)

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.main.web-application-type` | `none` | 기본 실행은 CLI 클라이언트입니다. |
| `spring.ai.cli.step` | `ch5-final` | 기본 실행 러너입니다. |
| `spring.ai.model.chat` | `ollama` | 클라이언트 채팅 모델 제공자입니다. |
| `spring.ai.model.embedding` | `none` | 클라이언트는 직접 임베딩하지 않으므로 임베딩 모델 자동 구성을 끕니다. |
| `spring.ai.mcp.client.enabled` | `true` | MCP 클라이언트를 켭니다. |
| `spring.ai.mcp.client.type` | `SYNC` | 동기 MCP 클라이언트를 사용합니다. |
| `spring.ai.mcp.client.toolcallback.enabled` | `true` | MCP 서버 툴을 Spring AI `ToolCallback`으로 노출합니다. |
| `spring.ai.mcp.client.streamable-http.connections.rag.url` | `http://localhost:8085` | RAG MCP 서버 주소입니다. |
| `spring.ai.mcp.client.streamable-http.connections.rag.endpoint` | `/mcp` | MCP Streamable HTTP 엔드포인트입니다. |
| `spring.ai.mcp.server.enabled` | `false` | 클라이언트 실행에서는 서버 기능을 끕니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | 클라이언트 CLI가 사용자와 대화할 때 쓰는 채팅 모델입니다. |

### 서버 설정 (application.yml의 server 프로파일 문서)

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `server.port` | `8085` | MCP 서버 포트입니다. |
| `spring.config.activate.on-profile` | `server` | `--spring.profiles.active=server`일 때만 적용됩니다. |
| `spring.main.web-application-type` | `servlet` | MCP Streamable HTTP 서버를 위해 웹 서버를 띄웁니다. |
| `spring.ai.model.chat` | `ollama` | 서버 답변 생성용 채팅 모델 제공자입니다. |
| `spring.ai.model.embedding` | `ollama` | 서버 RAG 임베딩 모델 제공자입니다. |
| `spring.ai.ollama.embedding.model` | `bge-m3` | 서버 RAG 지식 베이스의 임베딩 모델입니다. |
| `spring.ai.mcp.client.enabled` | `false` | 서버 실행에서는 MCP 클라이언트를 끕니다. |
| `spring.ai.mcp.server.enabled` | `true` | MCP 서버를 켭니다. |
| `spring.ai.mcp.server.name` | `chapter5-rag-mcp-server` | 클라이언트에 노출되는 MCP 서버 이름입니다. |
| `spring.ai.mcp.server.protocol` | `STREAMABLE` | Streamable HTTP MCP 서버로 실행합니다. |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint` | `/mcp` | 서버가 받는 MCP 엔드포인트입니다. |

## OpenAI API 사용

5장은 클라이언트 채팅 모델과 서버 RAG 임베딩/답변 모델을 모두 사용합니다. OpenAI로 실행하려면 OpenAI starter를 추가하고, 클라이언트와 서버 설정에 OpenAI 모델을 지정합니다.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

클라이언트 `application.yml` 예시입니다.

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

서버 설정(`application.yml`의 `server` 프로파일 문서) 예시입니다.

```yaml
spring:
  ai:
    model:
      chat: openai
      embedding: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4.1-nano
      embedding:
        model: text-embedding-3-small
```

> **모델 선택**: 실습 기본값은 비추론(non-reasoning) 모델 `gpt-4.1-nano`입니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model`, `spring.ai.openai.embedding.model` 값만 교체하면 됩니다. `gpt-5` 계열도 Spring AI 2.0.0에서 동작하지만 추론 토큰 과금과 응답 지연이 있어, 실습에는 gpt-4 계열(`gpt-4.1-nano`, `gpt-4o-mini`)을 권장합니다.

> **OpenAI 전환 시 5장 주의(실측)**: MCP 연결, 툴 발견, 툴 호출은 gpt-5-nano 포함 OpenAI에서 정상 동작합니다. 다만 서버 RAG 검색 임계값(`RagSearchService`/`RagAnswerService`의 `DEFAULT_SIMILARITY_THRESHOLD = 0.50`)이 `bge-m3` 기준이라, `text-embedding-3-small`은 유사도 점수 분포가 더 낮아 유효한 문서도 임계값 아래로 떨어져 검색이 비는 경우가 많습니다. OpenAI 임베딩으로 실습할 때는 이 임계값을 `0.3` 수준으로 낮추는 것을 권장합니다. 또한 서버 프로파일에도 `OPENAI_API_KEY`를 반드시 설정해야 기동됩니다.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server --spring.ai.cli.step=ch5-server-step4"
```

## 확인 포인트

- 서버 프로파일에서는 MCP 클라이언트가 꺼지고, MCP 서버와 임베딩이 켜집니다.
- 기본 클라이언트 실행에서는 MCP 서버가 꺼지고, `streamable-http` 클라이언트가 `localhost:8085/mcp`에 연결됩니다.
- 5장은 MCP의 서버/클라이언트 경계를 보여주는 장이므로 서버 터미널과 클라이언트 터미널을 분리해서 실행하는 것이 좋습니다.

## 통합 테스트 (IT)

라이브 실행 테스트는 서버(`Chapter5ServerStepIT`)와 클라이언트(`Chapter5ClientStepIT`)로 나뉜 통합 테스트입니다. `./mvnw test`는 Ollama가 필요 없는 단위 테스트(`Chapter5RagDocumentLoaderTests`)만 실행합니다.

### 사전 요구사항

- Ollama 실행 중 (`http://localhost:11434`)
- `qwen3.5:4b` 채팅 모델과 `bge-m3` 임베딩 모델 설치
- 포트 8085가 비어 있을 것 (클라이언트 IT가 MCP 서버를 자체 기동)

### 실행 방법

```bash
# 전체 IT 실행 (클라이언트 IT는 @BeforeAll에서 서버를 띄우고 끝나면 내림)
./mvnw verify

# 특정 스텝만 실행
./mvnw verify -Dit.test='Chapter5ServerStepIT#serverStep1_ragKnowledgeBase'
```

### 동작 방식

- 클라이언트 final IT는 책의 예시 대화(질문, /resource, /answer-direct)를 순서대로 자동 입력합니다 (`SimulatedConsoleInput`, 줄 사이 기본 0.4초).
- 입력 간격은 `-Dchapter5.simulation.line-delay-millis=500` 으로 조절할 수 있습니다.
- 스크립트 종료 후 `/exit` 자동 공급, 테스트당 10분 타임아웃.
- 주의: chapter6 IT와 포트 8085를 공유하므로 동시에 실행하지 마세요. 여러 모듈 IT 동시 실행은 Ollama 경합으로 타임아웃을 유발합니다.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 5.68 | 495 | 5장 프로젝트의 주요 의존성 | [`pom.xml`](pom.xml) |
| 5.69 | 495 | 5장 클라이언트 및 서버 분리 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 5.71 | 499 | 문서 준비 흐름의 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/support/RagDocumentLoader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/support/RagDocumentLoader.java) |
| 5.72 | 500 | MCP 서버의 벡터 저장소를 구성하는 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagConfig.java) |
| 5.73 | 500 | 기동 시 RAG 인덱싱 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/support/RagMcpKnowledgeBase.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/support/RagMcpKnowledgeBase.java) |
| 5.75 | 502 | Chapter5RagMcpSearchTools의 전체 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpSearchTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpSearchTools.java) |
| 5.76 | 504 | 검색 전용 패턴의 CLI 실행 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep2_SearchTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep2_SearchTools.java) |
| 5.77 | 505 | Chapter5RagMcpAnswerTools의 전체 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpAnswerTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpAnswerTools.java) |
| 5.78 | 506 | 서버 측 답변 패턴의 CLI 실행 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep3_AnswerTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep3_AnswerTools.java) |
| 5.79 | 507 | 리소스, 프롬프트, 자동 완성 프리미티브 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpPrimitiveConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/server/Chapter5RagMcpPrimitiveConfig.java) |
| 5.80 | 510 | 리소스, 프롬프트, 자동 완성 CLI 실행 주요 구현 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep4_McpServerPrimitives.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5ServerStep4_McpServerPrimitives.java) |
| 5.81 | 512 | MCP 서버 발견 Step의 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step1_McpDiscovery.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step1_McpDiscovery.java) |
| 5.82 | 513 | MCP 프리미티브를 직접 호출하는 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step2_McpPrimitiveCalls.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step2_McpPrimitiveCalls.java) |
| 5.83 | 515 | MCP 툴을 ToolCallback으로 변환하는 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step3_ToolCallbackProvider.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5Step3_ToolCallbackProvider.java) |
| 5.84 | 516 | 툴 필터링과 _meta 정책 적용 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/client/Chapter5McpClientConfiguration.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/client/Chapter5McpClientConfiguration.java) |
| 5.85 | 518 | 최종 MCP RAG ChatClient를 구성하는 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/client/McpEnabledChatService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/client/McpEnabledChatService.java) |
| 5.86 | 518 | MCP 기반 AI 챗봇 CLI의 명령 처리 부분의 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5McpCliChatbotApplication.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5McpCliChatbotApplication.java) |
| 5.87 | 519 | MCP 기반 AI 챗봇 CLI의 슬래시 명령어 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5McpCliChatbotApplication.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter5/Ch5McpCliChatbotApplication.java) |
<!-- book-examples:end -->
