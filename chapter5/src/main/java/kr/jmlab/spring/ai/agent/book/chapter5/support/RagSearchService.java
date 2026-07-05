package kr.jmlab.spring.ai.agent.book.chapter5.support;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 검색 전용 패턴 서비스. LLM 호출 없이 벡터 검색 결과만 반환한다.
 *
 * <p>이 서비스는 클라이언트 측 답변 패턴에서 사용된다. MCP 서버는 검색만 담당하고,
 * 검색 결과(sources)를 받은 클라이언트의 ChatClient 가 답변을 직접 구성한다.</p>
 */
@Service
@Profile("server")
public class RagSearchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;

    private final VectorStore vectorStore;

    public RagSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public RagSearchResponse search(String query, Integer topK, String category) {
        List<Document> documents = retrieve(query, topK, category);
        return new RagSearchResponse(
                query,
                normalizeCategory(category),
                documents.size(),
                documents.stream()
                        .map(this::toSearchResult)
                        .toList());
    }

    private List<Document> retrieve(String query, Integer topK, String category) {
        // 인덱싱은 RagMcpKnowledgeBase 의 @PostConstruct 에서 서버 기동 시 이미 완료되었다.
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(normalizeTopK(topK))
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request).stream()
                .filter(document -> "true".equals(String.valueOf(document.getMetadata().get("isActive"))))
                .filter(document -> matchesCategory(document, category))
                .toList();

        // KeywordFilteringPostProcessor 와 동일한 의도를 MCP 서버 안에서 단순 인라인으로 처리한다.
        if (query.contains("긴급")) {
            return documents.stream()
                    .filter(document -> document.getText().contains("긴급"))
                    .toList();
        }
        return documents;
    }

    private RagSearchResult toSearchResult(Document document) {
        return new RagSearchResult(
                String.valueOf(document.getMetadata().get("source")),
                String.valueOf(document.getMetadata().get("category")),
                document.getScore(),
                summarize(document.getText(), 360),
                document.getMetadata());
    }

    private boolean matchesCategory(Document document, String category) {
        String normalized = normalizeCategory(category);
        if (normalized.isBlank()) {
            return true;
        }
        return normalized.equals(String.valueOf(document.getMetadata().get("category")));
    }

    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, 10);
    }

    private String summarize(String text, int maxLength) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    public record RagSearchResponse(
            String query,
            String category,
            int resultCount,
            List<RagSearchResult> results) {
    }

    public record RagSearchResult(
            String source,
            String category,
            double score,
            String text,
            Map<String, Object> metadata) {
    }
}
