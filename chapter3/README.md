# chapter3

3장 프로젝트는 로컬 문서를 읽고, 정제하고, 벡터 저장소에 적재한 뒤 RAG 챗봇으로 연결하는 예제입니다. Spring AI의 DocumentReader, DocumentTransformer, DocumentWriter, VectorStore, Advanced RAG 구성을 단계별로 확인합니다.

## 전제 조건

- Java 21
- Ollama 실행 중: `http://localhost:11434`
- 채팅 모델: `qwen3.5:4b`
- 임베딩 모델: `bge-m3`

```bash
ollama pull qwen3.5:4b
ollama pull bge-m3
```

## 실행

기본값은 최종 RAG CLI인 `ch3-final`입니다.

```bash
./mvnw spring-boot:run
```

특정 단계를 실행하려면 `spring.ai.cli.step` 값을 지정합니다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch3-step4"
```

## 단계

| step | 실행 클래스 | 내용 |
| --- | --- | --- |
| `ch3-step1` | `Ch3Step1_DocumentReaders` | 원천 문서 읽기 |
| `ch3-step2` | `Ch3Step2_DocumentTransformers` | 문서 정제와 청킹 |
| `ch3-step3` | `Ch3Step3_DocumentWriters` | ETL 파이프라인의 Load 단계 |
| `ch3-step4` | `Ch3Step4_VectorStore` | `SimpleVectorStore` 기반 임베딩과 유사도 검색 |
| `ch3-step5` | `Ch3Step5_AdvancedRagModules` | Advanced RAG 구성 요소 확인 |
| `ch3-step6` | `Ch3Step6_AdvancedRag` | `RetrievalAugmentationAdvisor` 기반 Advanced RAG |
| `ch3-final` | `Ch3RagCliChatbotApplication` | 3장 최종 RAG CLI 챗봇 |

## 주요 리소스

| 경로 | 역할 |
| --- | --- |
| `src/main/resources/data` | 실습용 문서 디렉터리 |
| `src/main/resources/application.yml` | RAG 문서 경로, Ollama 채팅/임베딩 모델 설정 |
| `Chapter3RagConfig` | 인프라 없이 실행하기 위한 `SimpleVectorStore` 구성 |

## yml 설정

| 설정 | 현재 값 | 설명 |
| --- | --- | --- |
| `spring.ai.cli.step` | `ch3-final` | 기본 실행 러너입니다. `ch3-step1`부터 `ch3-step6`까지 바꿔 실행할 수 있습니다. |
| `spring.ai.model.chat` | `ollama` | Spring AI 2.0 GA 방식의 채팅 모델 제공자 선택입니다. |
| `spring.ai.model.embedding` | `ollama` | Spring AI 2.0 GA 방식의 임베딩 모델 제공자 선택입니다. |
| `spring.ai.rag.documents-dir` | `src/main/resources/data` | RAG ETL이 읽는 문서 디렉터리입니다. |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | 로컬 Ollama 서버 주소입니다. |
| `spring.ai.ollama.chat.model` | `qwen3.5:4b` | RAG 답변 생성에 쓰는 채팅 모델입니다. |
| `spring.ai.ollama.embedding.model` | `bge-m3` | 문서 임베딩과 유사도 검색에 쓰는 임베딩 모델입니다. |

## OpenAI API 사용

RAG 예제는 채팅 모델과 임베딩 모델을 모두 사용합니다. OpenAI로 실행하려면 OpenAI starter를 추가하고 `chat`, `embedding` 모델 제공자를 모두 OpenAI로 지정합니다.

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
      embedding: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4.1-nano
      embedding:
        model: text-embedding-3-small
```

> **모델 선택**: 실습 기본값은 비추론(non-reasoning) 모델 `gpt-4.1-nano`입니다. 다른 모델로 바꾸려면 `spring.ai.openai.chat.model`, `spring.ai.openai.embedding.model` 값만 교체하면 됩니다. `gpt-5` 계열도 Spring AI 2.0.0에서 동작하지만 추론 토큰 과금과 응답 지연이 있어, 실습에는 gpt-4 계열(`gpt-4.1-nano`, `gpt-4o-mini`)을 권장합니다.

> **OpenAI 전환 시 3장 주의(실측)**: 임베딩을 `text-embedding-3-small`로 바꾸면, `RewriteQueryTransformer`가 한국어 질의를 재작성하는 과정에서 유사도가 임계값 아래로 내려가 검색이 비는 경우가 있습니다. 이때는 근거 문서가 없다는 안내로 정상 처리됩니다(빈-컨텍스트 템플릿은 플레이스홀더 없이 사용해야 합니다). 검색 적중을 높이려면 `VectorStoreDocumentRetriever`의 `similarityThreshold`를 낮추거나 쿼리 재작성 단계를 조정하세요.

```bash
export OPENAI_API_KEY=<your-openai-api-key>
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch3-final"
```

## 확인 포인트

- 문서 경로는 `spring.ai.rag.documents-dir=src/main/resources/data`로 설정되어 있습니다.
- 이 프로젝트의 벡터 저장소는 학습용 인메모리 `SimpleVectorStore`입니다.
- `ch3-final`은 실행 시 문서를 적재한 뒤 사용자 질문에 근거 문서를 활용해 답변합니다.

## 통합 테스트 (IT)

라이브 LLM과 임베딩이 필요한 스텝(4, 5, 6, final)은 통합 테스트(`Chapter3StepRunnerIT`)로 분리되어 있습니다. `./mvnw test`는 Ollama가 필요 없는 Step 1~3 단위 테스트(`Chapter3StepRunnerTests`)만 실행합니다.

### 사전 요구사항

- Ollama 실행 중 (`http://localhost:11434`)
- `qwen3.5:4b` 채팅 모델과 `bge-m3` 임베딩 모델 설치

### 실행 방법

```bash
# 전체 IT 실행
./mvnw verify

# 특정 스텝만 실행
./mvnw verify -Dit.test='Chapter3StepRunnerIT#step4_vectorStore'
```

### 동작 방식

- 책의 예시 질문을 여러 턴 자동 입력합니다 (`SimulatedCliInput`, 줄 사이 기본 0.4초).
- 입력 간격은 `-Dchapter3.sim.lineDelayMillis=500` 으로 조절할 수 있습니다.
- 스크립트 종료 후 `/exit` 자동 공급, 테스트당 15분 타임아웃 (문서 인덱싱 포함).
- 주의: 여러 모듈의 IT를 동시에 실행하면 Ollama 경합으로 타임아웃이 날 수 있습니다. 모듈 하나씩 실행하세요.

<!-- book-examples:start (자동 생성 구간) -->
## 책 예제와 저장소 파일

책의 예제 번호로 이 프로젝트의 파일을 찾을 수 있습니다. 스프링 AI 프레임워크 소스를 인용한 예제, 본문 속 짧은 코드 조각, 명령과 실행 결과는 목록에서 뺐습니다. 예제가 파일의 주요 부분만 보여 주는 경우 파일에는 전체 코드가 있습니다.

| 예제 | 쪽 | 제목 | 파일 |
| --- | --- | --- | --- |
| 3.1 | 142 | 텍스트 파일 읽기 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SimpleTextReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SimpleTextReader.java) |
| 3.2 | 144 | JSON 읽기 데이터 구조 샘플 | [`src/main/resources/data/bikes.json`](src/main/resources/data/bikes.json) |
| 3.3 | 145 | 다양한 JSON 처리 기법 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/JsonDocReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/JsonDocReader.java) |
| 3.4 | 148 | PDF 읽기 컴포넌트 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/PdfReaderComponent.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/PdfReaderComponent.java) |
| 3.5 | 150 | 원본 마크다운 파일 | [`src/main/resources/data/spring-boot-guide.md`](src/main/resources/data/spring-boot-guide.md) |
| 3.6 | 151 | 마크다운 읽기 처리 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/MarkdownDocsReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/MarkdownDocsReader.java) |
| 3.8 | 154 | HTML 읽기 처리 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/WebPageReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/WebPageReader.java) |
| 3.9 | 157 | 범용 문서 읽기 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/OfficeFileReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/OfficeFileReader.java) |
| 3.11 | 161 | 데이터베이스 게시글 읽기 커스텀 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomDatabaseReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomDatabaseReader.java) |
| 3.12 | 162 | 외부 REST API 데이터 연동 커스텀 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/ApiDocumentReader.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/ApiDocumentReader.java) |
| 3.13 | 166 | TokenTextSplitter 생성 방법 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/MyTokenTextSplitter.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/MyTokenTextSplitter.java) |
| 3.15 | 170 | DefaultContentFormatter 커스텀 설정 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomContentFormatExample.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomContentFormatExample.java) |
| 3.16 | 172 | KeywordMetadataEnricher 기본 사용 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/KeywordEnricherExample.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/KeywordEnricherExample.java) |
| 3.17 | 172 | KeywordMetadataEnricher 커스텀 템플릿 사용 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomKeywordEnricher.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomKeywordEnricher.java) |
| 3.18 | 175 | SummaryMetadataEnricher 기본 사용 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SummaryEnricherExample.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SummaryEnricherExample.java) |
| 3.19 | 177 | SummaryMetadataEnricher 커스텀 템플릿 사용 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomSummaryEnricher.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/CustomSummaryEnricher.java) |
| 3.21 | 179 | 문자 길이를 기준으로 문서를 분할하는 DocumentTransformer | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SimpleLengthSplitter.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/SimpleLengthSplitter.java) |
| 3.22 | 180 | 마스킹을 수행하는 DocumentTransformer 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/PiiMaskingTransformer.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/PiiMaskingTransformer.java) |
| 3.23 | 182 | DocumentTransformer 통합 파이프라인 서비스 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/DocumentProcessingPipeline.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/DocumentProcessingPipeline.java) |
| 3.24 | 185 | FileDocumentWriter를 활용한 ETL 파이프라인 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/FileEtlPipeline.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/FileEtlPipeline.java) |
| 3.26 | 187 | 로거를 활용한 사용자 정의 DocumentWriter | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/LoggingDocumentWriter.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/LoggingDocumentWriter.java) |
| 3.45 | 236 | QuestionAnswerAdvisor를 사용한 Naive RAG 구현 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/NaiveRagService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/NaiveRagService.java) |
| 3.49 | 243 | Advanced RAG를 구현한 주요 코드 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java) |
| 3.54 | 252 | 커스텀 DocumentPostProcessor 구현과 RetrievalAugmentationAdvisor 설정 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/KeywordFilteringPostProcessor.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/KeywordFilteringPostProcessor.java) |
| 3.58 | 259 | 3장 프로젝트의 주요 의존성 | [`pom.xml`](pom.xml) |
| 3.59 | 259 | 3장 프로젝트 설정 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
| 3.60 | 261 | 데이터 추출 구현 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step1_DocumentReaders.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step1_DocumentReaders.java) |
| 3.61 | 262 | 정제와 청킹 구현 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step2_DocumentTransformers.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step2_DocumentTransformers.java) |
| 3.62 | 263 | 적재 단계 구현 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step3_DocumentWriters.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step3_DocumentWriters.java) |
| 3.63 | 264 | 임베딩과 VectorStore 인덱싱을 구현한 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step4_VectorStore.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step4_VectorStore.java) |
| 3.64 | 266 | Advanced RAG Advisor 조립을 구현한 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java) |
| 3.65 | 267 | Advanced RAG 실행 구현 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step6_AdvancedRag.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step6_AdvancedRag.java) |
| 3.66 | 268 | RAG CLI 챗봇의 오프라인 인덱싱을 구현한 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/support/RagKnowledgeBase.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/support/RagKnowledgeBase.java) |
| 3.67 | 269 | RAG CLI 챗봇의 런타임을 구현한 주요 부분 | [`src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3RagCliChatbotApplication.java`](src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3RagCliChatbotApplication.java) |
<!-- book-examples:end -->
