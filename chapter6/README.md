# chapter6

6장 프로젝트는 Spring AI 기반 에이전트 구조를 단계별로 확장하는 예제입니다. 로컬 툴, MCP 원격 툴, 승인 게이트, agent-as-a-tool, 작업 계획 툴, 하위 에이전트/스킬/동적 툴 발견, 관측 설정을 하나의 프로젝트에서 확인합니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`
- 임베딩 모델: `bge-m3`

```bash
ollama pull qwen3.5:4b
ollama pull bge-m3
```

## 로컬 단계 실행

MCP 서버 없이 로컬 기능만 확인하는 단계는 MCP 클라이언트를 끄고 실행할 수 있습니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-step1 --spring.ai.mcp.client.enabled=false"
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-step5 --spring.ai.mcp.client.enabled=false"
```

## MCP 서버 실행

운영 MCP 서버는 `ops` 프로파일로 실행하며 기본 포트는 `8085`입니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=ops"
```

지식 MCP 서버는 `knowledge` 프로파일로 실행하며 기본 포트는 `8086`입니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=knowledge"
```

## 클라이언트 실행

기본값은 최종 통합 CLI인 `ch6-final`입니다. `ch6-step2`, `ch6-step3`, `ch6-step6`, `ch6-final`은 운영 서버가 먼저 떠 있어야 합니다.

```bash
./mvnw spring-boot:run
```

특정 단계를 실행하려면 `spring.ai.cli.step` 값을 지정합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-step2"
```

`ch6-step4`처럼 지식 서버까지 사용하는 단계는 지식 서버도 먼저 띄우고, 책 예제 6.42처럼 `application.yml`의 `knowledge` 연결 주석을 해제하거나 다음처럼 실행 인자로 URL을 넘깁니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-step4 --spring.ai.mcp.client.streamable-http.connections.knowledge.url=http://localhost:8086"
```

## 단계

| step | 실행 클래스 | 필요 서버 | 내용 |
| --- | --- | --- | --- |
| `ch6-step1` | `Ch6Step1_SpringAIAgent` | 없음 | 로컬 툴 기반 기본 에이전트 |
| `ch6-step2` | `Ch6Step2_McpConnect` | ops | MCP 원격 툴 연결 |
| `ch6-step3` | `Ch6Step3_ApprovalGate` | ops | 승인 프롬프트가 있는 툴 호출 |
| `ch6-step4` | `Ch6Step4_AgentAsTool` | ops, knowledge | 지식 서버의 RAG 에이전트를 툴처럼 호출 |
| `ch6-step5` | `Ch6Step5_TaskTool` | 없음 | `TaskTool`로 보고서 작성을 하위 에이전트(report-writer)에 위임 |
| `ch6-step6` | `Ch6Step6_Orchestration` | ops | 오케스트레이션과 승인 흐름 |
| `ch6-final` | `Ch6EnhancedSpringAIAgentCli` | ops, 선택적으로 knowledge | 최종 통합 에이전트 CLI |

## 주요 구성

| 경로 | 역할 |
| --- | --- |
| `capability/local` | 계산, 날짜, 재고 등 로컬 툴 |
| `capability/remote` | 운영/지식 MCP 서버와 원격 툴 |
| `channel` | 장별 CLI 실행 클래스 |
| `orchestration` | 에이전트 구성, 안전장치, 추적 어드바이저 |
| `src/main/resources/agents` | 하위 에이전트 프롬프트 |
| `src/main/resources/skills` | 스킬 기반 절차 문서 |

## yml 설정

### 클라이언트 application.yml

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.application.name` | `chapter6-agent-client` | 기본 클라이언트 애플리케이션 이름입니다. |
| `spring.main.web-application-type` | `none` | 기본 실행은 CLI 클라이언트입니다. |
| `spring.ai.cli.step` | `ch6-final` | 옵션 없이 실행할 때 최종 통합 CLI를 실행합니다. |
| `spring.ai.cli.agent.core.system-prompt` | 긴 시스템 프롬프트 | Step1 등 기본 에이전트 페르소나입니다. |
| `spring.ai.cli.agent.enhanced.system-prompt` | 긴 시스템 프롬프트 | final 에이전트의 스킬, 계획, 명확화, 하위 에이전트 사용 정책입니다. |
| `spring.ai.model.chat` | `ollama` | Spring AI 2.0 GA 방식의 채팅 모델 제공자 선택입니다. |
| `spring.ai.model.embedding` | `ollama` | Spring AI 2.0 GA 방식의 임베딩 모델 제공자 선택입니다. |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | 로컬 Ollama 서버 주소입니다. |
| `spring.ai.ollama.chat.think` | `low` | Ollama 모델의 thinking 수준을 낮게 둡니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | 에이전트의 기본 채팅 모델입니다. |
| `spring.ai.ollama.embedding.model` | `bge-m3` | 동적 툴 색인과 RAG용 임베딩 모델입니다. |
| `spring.ai.mcp.client.enabled` | `true` | 원격 MCP 서버 연결을 켭니다. |
| `spring.ai.mcp.client.toolcallback.enabled` | `true` | 원격 MCP 툴을 에이전트의 툴로 노출합니다. |
| `spring.ai.mcp.client.streamable-http.connections.operations.url` | `http://localhost:8085` | 운영 MCP 서버 주소입니다. |
| `spring.ai.mcp.client.streamable-http.connections.knowledge.url` | 주석 처리 | Step4/final에서 지식 서버가 필요하면 `http://localhost:8086`으로 켭니다. |
| `management.otlp.*` | `localhost:4318` | OTLP 트레이스/메트릭 내보내기 설정입니다. 수집기가 없으면 관측 데이터만 전송되지 않습니다. |

### 운영 서버 application-ops.yml

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `server.port` | `8085` | 운영 MCP 서버 포트입니다. |
| `spring.config.activate.on-profile` | `ops` | `--spring.profiles.active=ops`일 때만 적용됩니다. |
| `spring.main.web-application-type` | `servlet` | MCP 서버를 HTTP로 제공하기 위해 웹 서버를 띄웁니다. |
| `spring.ai.mcp.client.enabled` | `false` | 서버 실행에서는 MCP 클라이언트를 끕니다. |
| `spring.ai.mcp.server.enabled` | `true` | MCP 서버를 켭니다. |
| `spring.ai.mcp.server.name` | `chapter6-operations-mcp-server` | 운영 툴을 제공하는 MCP 서버 이름입니다. |
| `spring.ai.mcp.server.protocol` | `STREAMABLE` | Streamable HTTP MCP 서버로 실행합니다. |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint` | `/mcp` | MCP 엔드포인트입니다. |

### 지식 서버 application-knowledge.yml

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `server.port` | `8086` | 지식 MCP 서버 포트입니다. |
| `spring.config.activate.on-profile` | `knowledge` | `--spring.profiles.active=knowledge`일 때만 적용됩니다. |
| `spring.main.web-application-type` | `servlet` | MCP 서버를 HTTP로 제공하기 위해 웹 서버를 띄웁니다. |
| `spring.ai.rag.documents-dir` | `src/main/resources/data` | 지식 서버가 읽는 RAG 문서 디렉터리입니다. |
| `spring.ai.model.chat` | `ollama` | 지식 서버 내부 하위 에이전트의 채팅 모델 제공자입니다. |
| `spring.ai.model.embedding` | `ollama` | 지식 서버 RAG 임베딩 모델 제공자입니다. |
| `spring.ai.ollama.embedding.model` | `bge-m3` | 지식 서버 RAG 임베딩 모델입니다. |
| `spring.ai.mcp.server.name` | `chapter6-knowledge-mcp-server` | RAG agent-as-a-tool을 제공하는 MCP 서버 이름입니다. |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint` | `/mcp` | MCP 엔드포인트입니다. |

## OpenAI API 사용

6장은 채팅 모델, 임베딩 모델, Tool Calling, MCP 툴 호출을 함께 사용합니다. OpenAI로 실행하려면 OpenAI starter를 추가하고, 클라이언트와 지식 서버 모두에 OpenAI 설정을 적용합니다.

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
      embedding: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4.1-nano
      embedding:
        model: text-embedding-3-small
```

지식 서버 `application-knowledge.yml`도 RAG 답변과 임베딩을 사용하므로 같은 OpenAI 설정을 넣습니다. 운영 서버 `application-ops.yml`은 로컬 운영 툴만 제공하므로 별도 모델 설정이 핵심은 아닙니다.

> **모델 선택**: 실습 기본값은 비추론(non-reasoning) 모델 `gpt-4.1-nano`입니다. 이 모델은 툴 호출을 지원하므로 6장 에이전트 예제가 정상 동작합니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model`, `spring.ai.openai.embedding.model` 값만 교체하면 됩니다. `gpt-5` 계열도 Spring AI 2.0.0에서 동작하지만 추론 토큰 과금과 응답 지연이 있어, 실습에는 gpt-4 계열(`gpt-4.1-nano`, `gpt-4o-mini`)을 권장합니다.

> **OpenAI 전환 시 6장 주의(실측)**: 로컬 에이전트(`ch6-step1`), MCP 원격 툴(`ch6-step2`), 최종 강화 에이전트(`ch6-final`: 스킬, 동적 툴 검색, MCP, 승인 게이트)는 gpt-5-nano 포함 OpenAI에서 정상 동작합니다(에이전트 툴 루프가 `ToolCallingAdvisor` 기반 비스트리밍이라 4장의 스트리밍+툴 400 문제를 겪지 않음). 단 (1) `ops`, `knowledge` 서버를 포함한 **모든 프로세스에 `OPENAI_API_KEY`가 있어야 기동**되고, (2) 지식 서버의 RAG 검색 임계값(`0.50`)은 `bge-m3` 기준이라 `text-embedding-3-small`에선 검색이 비기 쉬우므로 `0.3` 수준으로 낮추길 권장합니다.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch6-final"
```

## 확인 포인트

- `application.yml`에는 운영 서버 연결이 기본으로 켜져 있고, 지식 서버 연결은 필요할 때 명령행 인자로 추가하거나 설정 파일에서 주석을 해제합니다.
- `ops`와 `knowledge` 프로파일은 웹 서버를 띄우고, 기본 클라이언트 프로파일은 콘솔형으로 실행됩니다.
- 관측 설정은 OTLP 엔드포인트를 가리키지만, 수집기가 없어도 핵심 실습 실행에는 영향이 없도록 구성되어 있습니다.

## 통합 테스트 (IT)

라이브 실행 테스트는 통합 테스트(`Chapter6ClientStepIT`)로 분리되어 있습니다. 각 스텝이 실제 Ollama로 에이전트를 구동합니다. `./mvnw test`는 Ollama가 필요 없는 단위 테스트만 실행합니다.

### 사전 요구사항

- Ollama 실행 중 (`http://localhost:11434`)
- `qwen3.5:4b` 채팅 모델과 `bge-m3` 임베딩 모델 설치
- 포트 8085(운영 서버), 8086(지식 서버)이 비어 있을 것 (IT가 서버들을 자체 기동)

### 실행 방법

```bash
# 전체 IT 실행 (지식 서버는 한 번만 기동해 공유, 운영 서버는 스텝마다 재기동)
./mvnw verify

# 특정 스텝만 실행 (예: 승인 게이트 시나리오)
./mvnw verify -Dit.test='Chapter6ClientStepIT#step3_approvalGate'
```

### 동작 방식

- 책의 예시 요청을 순서대로 자동 입력하며, 승인 게이트의 y/n 응답까지 스텝별 스크립트에 포함되어 있습니다 (줄 사이 기본 0.4초).
- 입력 간격은 `-Dch6.sim.line-delay-ms=500` 으로 조절할 수 있습니다.
- 스크립트 종료 후 `/exit` 자동 공급, 테스트당 15분 타임아웃.
- 주의: chapter5 IT와 포트 8085를 공유하므로 동시에 실행하지 마세요. 여러 모듈 IT 동시 실행은 Ollama 경합으로 타임아웃을 유발합니다.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 6.1 | 526 | 프롬프트 체이닝 워크플로의 주요 구현2 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/local/ChainWorkflow.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/local/ChainWorkflow.java) |
| 6.13 | 555 | 툴 루프 관측 어드바이저 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/ToolLoopMetricsAdvisor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/ToolLoopMetricsAdvisor.java) |
| 6.18 | 576 | Elicitation 핸들러를 이용한 승인 게이트 콘솔 환경 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java) |
| 6.19 | 580 | ToolSearchToolCallingAdvisor 의존성 추가 | [`pom.xml`](pom.xml) |
| 6.21 | 582 | spring-ai-agent-utils 의존성 추가 | [`pom.xml`](pom.xml) |
| 6.24 | 588 | AskUserQuestionTool 등록 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.27 | 595 | TaskTool로 하위 에이전트 등록 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.29 | 609 | 6장 주요 의존성 | [`pom.xml`](pom.xml) |
| 6.30 | 609 | 6장 코어 에이전트 주요 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 6.31 | 611 | 코어 에이전트 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java) |
| 6.32 | 613 | 코어 에이전트 구성 설정의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.33 | 614 | 에이전트 루프의 안전 가드 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentSafety.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentSafety.java) |
| 6.34 | 617 | 벡터 스토어를 생성하는 설정 부분 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.35 | 618 | MCP 서버 기반으로 스프링 AI 능력 확장을 위한 주요 의존성 | [`pom.xml`](pom.xml) |
| 6.36 | 619 | 운영 MCP 서버의 주요 설정 | [`src/main/resources/application-ops.yml`](src/main/resources/application-ops.yml) |
| 6.37 | 619 | 지식 MCP 서버의 주요 설정 | [`src/main/resources/application-knowledge.yml`](src/main/resources/application-knowledge.yml) |
| 6.38 | 620 | 운영 MCP 서버를 연결한 MCP 클라이언트의 주요 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 6.39 | 621 | 운영 서버의 check_stock 툴 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java) |
| 6.40 | 622 | 사용자 작업 승인 게이트를 포함한 place_purchase_order 툴 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/remote/OperationsMcpTools.java) |
| 6.41 | 623 | 스프링 AI 에이전트 CLI의 승인 게이트 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/ConsoleElicitationHandler.java) |
| 6.42 | 626 | 지식 MCP 서버까지 연결한 MCP 클라이언트의 주요 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 6.43 | 629 | ToolSearchToolCallingAdvisor 기반 툴 루프 오케스트레이션 어드바이저(OrchestrationToolCallingAdvisor. | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java) |
| 6.44 | 633 | 강화 에이전트 구성 설정의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.45 | 634 | TaskTool의 하위 에이전트 정의 | [`src/main/resources/agents/report-writer.md`](src/main/resources/agents/report-writer.md) |
| 6.46 | 637 | 메타 툴 묶음을 완성하는 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java) |
| 6.47 | 638 | 스킬 정의 | [`src/main/resources/skills/restock-policy/SKILL.md`](src/main/resources/skills/restock-policy/SKILL.md) |
| 6.48 | 643 | 통합 CLI의 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/Ch6EnhancedSpringAIAgentCli.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/channel/Ch6EnhancedSpringAIAgentCli.java) |
| 6.49 | 647 | 관측 가능성 의존성 추가 | [`pom.xml`](pom.xml) |
| 6.50 | 647 | 관측 가능성 설정 추가 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
<!-- book-examples:end -->
