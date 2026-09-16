# Spring AI Agent Book

책 **「스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드」**(위키북스, 2026)의 실습 코드 저장소입니다. 책 소개는 [출판사 도서 페이지](https://wikibook.co.kr/springai-agents/), 장별 핵심 정리는 [온라인 가이드](https://jm-lab.github.io/spring-ai-agent-book/)에서 볼 수 있습니다. Spring AI 2.0.0 GA를 기준으로, 로컬 Ollama 모델만으로 기본 채팅부터 RAG, 툴 호출, MCP, 엔터프라이즈 멀티 에이전트까지 단계별로 구현합니다.

각 장의 프로젝트는 독립적인 Maven 프로젝트이며, 폴더 안에서 `./mvnw spring-boot:run`으로 바로 실행할 수 있습니다.

## 프로젝트 구성

| 디렉터리 | 장 | 주제 | 상세 안내 |
| --- | --- | --- | --- |
| [`basic-chat/`](basic-chat/) | 2장 도입 | 가장 작은 `ChatClient` 콘솔 예제 | [`basic-chat/README.md`](basic-chat/README.md) |
| [`chapter2/`](chapter2/) | 2장 | 스트리밍, 프롬프트, 구조화한 출력, 대화 메모리, 어드바이저 기반 CLI 챗봇 | [`chapter2/README.md`](chapter2/README.md) |
| [`chapter3/`](chapter3/) | 3장 | 문서 ETL, 임베딩, 벡터 저장소, Advanced RAG 기반 CLI 챗봇 | [`chapter3/README.md`](chapter3/README.md) |
| [`chapter4/`](chapter4/) | 4장 | `@Tool`, 함수형 툴, `ToolCallingManager`, `ToolCallingAdvisor` | [`chapter4/README.md`](chapter4/README.md) |
| [`chapter5/`](chapter5/) | 5장 | RAG 기능을 MCP 서버로 노출하고 MCP 클라이언트에서 사용 | [`chapter5/README.md`](chapter5/README.md) |
| [`chapter6/`](chapter6/) | 6장 | 에이전트 루프, 승인 게이트, agent-as-a-tool, 스킬, 하위 에이전트, 메타 툴 오케스트레이션, 관측 | [`chapter6/README.md`](chapter6/README.md) |

## 요구사항

- Java 21
- Spring Boot 4.0.x / Spring AI 2.0.0 GA (Maven Wrapper 포함, 별도 설치 불필요)
- [Ollama](https://ollama.com) 실행 중 (`http://localhost:11434`)

```bash
ollama pull qwen3.5:4b   # 채팅 모델 (전 장 공통, 약 3.4GB)
ollama pull bge-m3       # 임베딩 모델 (3, 5, 6장 RAG)
```

## 빠른 시작

```bash
cd chapter2
./mvnw spring-boot:run
```

옵션 없이 실행하면 각 장의 최종 통합 CLI가 시작됩니다. 장 안의 단계별 러너는 `--spring.ai.cli.step` 인자로 선택합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch2-step1"
```

## 공통 설정 구조

각 프로젝트의 `application.yml`은 다음 흐름을 공유합니다.

| 설정 | 의미 |
| --- | --- |
| `spring.main.web-application-type` | 웹 서버를 띄우지 않고 콘솔로 실행할 때 `none`을 지정합니다(basic-chat, 5장과 6장 클라이언트). MCP 서버 프로파일은 `servlet`으로 바꿉니다. |
| `spring.ai.cli.step` | 한 프로젝트 안에서 장별 Step 러너와 최종 러너 중 하나만 켭니다. |
| `spring.ai.model.chat` | Spring AI 2.0 GA 방식의 채팅 모델 제공자 선택입니다. 기본값은 `ollama`입니다. |
| `spring.ai.model.embedding` | Spring AI 2.0 GA 방식의 임베딩 모델 제공자 선택입니다. RAG 장에서는 `ollama`, 임베딩을 쓰지 않는 클라이언트에서는 `none`을 사용합니다. |
| `spring.ai.ollama.base-url` | 로컬 Ollama 서버 주소입니다. 기본값은 `http://localhost:11434`입니다. |
| `spring.ai.ollama.chat.model` | 이 저장소의 기본 채팅 모델입니다. 현재 값은 `qwen3.5:4b`입니다. |
| `spring.ai.ollama.embedding.model` | RAG, MCP RAG, 동적 툴 검색에서 쓰는 임베딩 모델입니다. 현재 값은 `bge-m3`입니다. |
| `spring.ai.rag.documents-dir` | RAG 예제가 읽는 문서 디렉터리입니다. |
| `spring.ai.mcp.*` | 5장/6장의 MCP 클라이언트와 서버 연결 설정입니다. |

## 테스트 구성

모든 장 모듈은 테스트를 두 종류로 나눕니다.

| 종류 | 이름 규칙 | 실행 | 요구사항 |
| --- | --- | --- | --- |
| 단위 테스트 | `*Tests` | `./mvnw test` | 없음 (Ollama 불필요, 빠름) |
| 통합 테스트 | `*IT` | `./mvnw verify` | Ollama 실행 중 + 해당 장의 모델 설치 |

통합 테스트는 사람이 입력하지 않아도 책의 예시 질문을 여러 턴 자동 입력하는 시뮬레이션 방식으로 동작하며, 테스트당 10~15분 타임아웃이 있어 입력 대기로 멈추지 않습니다. 다만 응답의 정확성까지 자동으로 단언하지는 않습니다. 흐름이 끝까지 실행되는지만 확인하고(서버 기동 실패 같은 하드 오류는 예외로 드러나 테스트가 깨집니다), 응답 내용과 품질은 출력을 사람이 보고 판단합니다. 자세한 실행 방법과 사전 요구사항은 각 장 README의 "통합 테스트 (IT)" 절을 참고하세요.

주의: 여러 모듈의 통합 테스트를 동시에 실행하면 Ollama 경합으로 응답이 느려져 타임아웃이 날 수 있습니다. 모듈 하나씩 실행하세요 (chapter5와 chapter6은 포트 8085도 공유합니다).

## MCP 서버를 외부 AI 클라이언트에 연결하기

이 저장소의 MCP 서버는 표준 Streamable HTTP로 노출되므로, 책의 CLI 클라이언트뿐 아니라 Claude Code, Claude Desktop, OpenAI Codex, OpenClaw 같은 외부 AI 에이전트에서도 그대로 연결해 쓸 수 있습니다.

### 1. 서버 띄우기

| 서버 | 실행 명령 | 엔드포인트 | 제공 툴 |
| --- | --- | --- | --- |
| 5장 RAG 서버 | `cd chapter5 && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server --spring.ai.cli.step=ch5-server-step4"` | `http://localhost:8085/mcp` | RAG 검색과 근거 답변 툴 |
| 6장 지식 서버 | `cd chapter6 && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=knowledge"` | `http://localhost:8086/mcp` | `rag_answer_question` (agent-as-a-tool) |
| 6장 운영 서버 | `cd chapter6 && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=ops"` | `http://localhost:8085/mcp` | `check_stock`, `place_purchase_order` |

RAG 서버들은 기동 시 문서를 인덱싱하므로 Ollama의 `bge-m3` 임베딩 모델이 필요합니다. 5장 서버와 6장 운영 서버는 같은 8085 포트를 쓰므로 동시에 띄우지 마세요.

### 2. Claude Code

```bash
claude mcp add --transport http book-knowledge http://localhost:8086/mcp
```

등록 후 Claude Code 대화에서 "재고 정책 문서에서 안전재고 기준을 찾아줘"처럼 물으면 서버 툴이 호출됩니다. `/mcp` 명령으로 연결 상태를 확인할 수 있습니다.

### 3. Claude Desktop

설정 > 커넥터(Connectors) > 커스텀 커넥터 추가에서 URL로 `http://localhost:8086/mcp`를 등록합니다. 구성 파일 방식을 쓰려면 `claude_desktop_config.json`에 [mcp-remote](https://www.npmjs.com/package/mcp-remote) 브리지로 등록할 수도 있습니다.

```json
{
  "mcpServers": {
    "book-knowledge": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8086/mcp"]
    }
  }
}
```

### 4. OpenAI Codex

`~/.codex/config.toml`에 Streamable HTTP 서버를 등록합니다.

```toml
[mcp_servers.book-knowledge]
url = "http://localhost:8086/mcp"
```

Codex 버전에 따라 `codex mcp add` 명령으로도 URL 서버를 등록할 수 있습니다. 자세한 옵션은 [Codex MCP 문서](https://developers.openai.com/codex/mcp)를 참고하세요.

### 5. OpenClaw

CLI 한 줄로 등록할 수 있습니다.

```bash
openclaw mcp set book-knowledge '{"url":"http://localhost:8086/mcp","transport":"streamable-http"}'
```

설정 파일 방식은 다음과 같습니다.

```json
{
  "mcp": {
    "servers": {
      "book-knowledge": {
        "url": "http://localhost:8086/mcp",
        "transport": "streamable-http"
      }
    }
  }
}
```

### 연결 시 주의사항

- 6장 운영 서버의 `place_purchase_order`는 실행 전에 MCP Elicitation으로 사용자 승인을 요청합니다. Elicitation을 지원하지 않는 클라이언트에서는 이 툴의 승인 단계가 진행되지 않을 수 있으니, 그 경우 `check_stock` 같은 조회 툴로 먼저 확인하세요.
- 서버는 인증 없이 localhost로 열립니다. 외부에 노출하려면 책 5.4절의 MCP 시큐리티 구성을 적용하세요.
- 클라이언트가 원격 컨테이너나 다른 머신에서 실행된다면 `localhost` 대신 접근 가능한 호스트 주소로 바꿔야 합니다.

## OpenAI API로 실행하기

현재 프로젝트들은 기본적으로 `spring-ai-starter-model-ollama`를 사용합니다. OpenAI API를 사용하려면 각 프로젝트의 `pom.xml`에서 모델 starter를 OpenAI용으로 바꾸거나 추가합니다.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

Spring AI 2.0.0 GA 기준 OpenAI 공통 설정은 다음 형태입니다.

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

채팅만 쓰는 `basic-chat`, `chapter2`, `chapter4`는 `embedding` 설정이 필요 없습니다. RAG와 MCP RAG를 쓰는 `chapter3`, `chapter5`, `chapter6`은 채팅 모델과 임베딩 모델을 함께 설정합니다.

> **실습 기본 모델**: 위 예시의 `gpt-4.1-nano`는 비추론(non-reasoning) 모델로, 응답 지연과 토큰 비용이 예측 가능하고 스트리밍, 구조화한 출력, 툴 호출, RAG에 모두 무리 없이 동작하는 저렴한 모델입니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model`(필요하면 `spring.ai.openai.embedding.model`) 값만 교체하면 됩니다. Spring AI 2.0.0 GA의 기본 OpenAI 채팅 모델은 `gpt-4o-mini`입니다. `gpt-5-nano` 같은 gpt-5 계열도 Spring AI 2.0.0에서 그대로 동작하지만, 추론 토큰 과금과 응답 지연이 있으므로 실습 기본값으로는 gpt-4 계열을 권장합니다.

> **주의(전 챕터 공통, 실측)**: `spring-ai-starter-model-openai`를 추가하면 chat, embedding 외에 audio 등 다른 OpenAI 모델 자동 구성도 활성화되고, audio speech 빈은 기동 시점에 자격 증명을 요구합니다. 따라서 **OpenAI를 직접 호출하지 않는 프로세스(예: 5, 6장 MCP 서버)를 띄울 때도 `OPENAI_API_KEY`(또는 `--spring.ai.openai.api-key`)를 반드시 설정**해야 애플리케이션이 기동됩니다.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run
```

OpenAI 모델을 쓸 때도 책의 예제 코드는 대부분 `ChatClient`, `EmbeddingModel`, `VectorStore`, `ToolCallback` 같은 Spring AI 추상화에 의존하므로 핵심 Java 코드는 그대로 두고 설정과 의존성 중심으로 바꾸면 됩니다.

## 공식 문서

- [Spring AI 2.0.0 레퍼런스](https://docs.spring.io/spring-ai/reference/)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
- [Spring AI Ollama Chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Spring AI OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
- [Model Context Protocol](https://modelcontextprotocol.io)

## 저작권

Copyright 2026 허제민(Jemin Huh)
