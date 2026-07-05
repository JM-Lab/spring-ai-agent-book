package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagDocumentLoader.IndexReport;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagDocumentLoader.PreparedDocuments;

/**
 * MCP 서버가 소유하는 RAG 지식 베이스.
 *
 * <p>서버가 기동되면 @PostConstruct 에서 즉시 문서를 인덱싱한다. MCP 클라이언트가 처음 툴을 호출할 때
 * 큰 지연이 붙는 것을 막고, 인덱싱 실패는 서버 기동 자체를 실패시켜 빠르게 드러낸다.</p>
 */
@Component
@Profile("knowledge")
public class KnowledgeBase {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBase.class);

    private final RagDocumentLoader documentLoader;
    private final VectorStore vectorStore;
    private final Path documentsDirectory;
    private boolean indexed;
    private IndexReport lastIndexReport;

    public KnowledgeBase(
            @Value("${spring.ai.rag.documents-dir:src/main/resources/data}") String documentsDirectory,
            RagDocumentLoader documentLoader,
            VectorStore vectorStore) {
        this.documentsDirectory = Path.of(documentsDirectory);
        this.documentLoader = documentLoader;
        this.vectorStore = vectorStore;
    }

    /**
     * 서버 기동 시 즉시 인덱싱을 수행한다. JSR-250 의 @PostConstruct 계약을 따르기 위해 void 를 반환한다.
     */
    @PostConstruct
    synchronized void indexOnStartup() {
        if (indexed) {
            return;
        }
        logger.info("Indexing RAG documents from {}", documentsDirectory.toAbsolutePath());
        PreparedDocuments preparedDocuments = documentLoader.prepare(documentsDirectory);
        if (preparedDocuments.chunks().isEmpty()) {
            throw new IllegalStateException("인덱싱할 문서가 없습니다: " + documentsDirectory);
        }

        vectorStore.add(preparedDocuments.chunks());
        lastIndexReport = preparedDocuments.report();
        indexed = true;
        logger.info("Indexed {} chunks from {} source documents",
                lastIndexReport.chunkCount(),
                lastIndexReport.sourceDocuments().size());
    }

    /**
     * 인덱싱 결과 보고서를 반환한다. @PostConstruct 가 실행되지 않은 경우를 대비해 안전망으로 인덱싱을 한 번 더 시도한다.
     */
    public synchronized IndexReport getIndexReport() {
        if (!indexed) {
            indexOnStartup();
        }
        return lastIndexReport;
    }

    /**
     * MCP 리소스 rag://pipeline 에서 노출하는 RAG 파이프라인 설명.
     */
    public String pipelineDescription() {
        return """
                RAG MCP Server Pipeline
                1. DocumentReader: txt, json, markdown, html 문서를 Document로 변환
                2. DocumentTransformer: 이메일/전화번호 PII 마스킹, 메타데이터 통일
                3. Split: TokenTextSplitter(chunkSize=300, minChunkSizeChars=80)
                4. VectorStore: SimpleVectorStore + Ollama bge-m3 embedding
                5. Retrieval: SearchRequest(topK, similarityThreshold=0.50, category filter)
                6. Answer: 검색 문서만 컨텍스트로 사용해 근거 기반 답변 생성
                """;
    }

    /**
     * MCP 자동완성 프리미티브에서 사용할 카테고리 후보를 반환한다.
     */
    public List<String> categorySuggestions(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return List.of("tech_docs", "product_catalog", "web_docs").stream()
                .filter(category -> category.startsWith(normalized))
                .toList();
    }
}
