package kr.jmlab.spring.ai.agent.book.chapter5.support;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 서버 측 답변 패턴 서비스. 검색과 LLM 답변 생성을 서버 내부에서 모두 수행한다.
 *
 * <p>이 서비스는 서버 측 답변 패턴에서 사용된다. MCP 서버는 RAG 문서를 검색한 뒤
 * 서버 내부 ChatClient 로 답변까지 만들어 클라이언트에 완성된 답변을 돌려준다.</p>
 */
@Service
@Profile("server")
public class RagAnswerService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagAnswerService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.clone()
                .defaultSystem("""
                        당신은 MCP 서버 안에서 실행되는 RAG 답변 생성기입니다.
                        제공된 [검색 문서]만 근거로 한국어 답변을 작성합니다.
                        문서에 없는 내용은 추측하지 말고, 알 수 없다고 답합니다.
                        답변 끝에는 사용한 출처 파일명을 짧게 정리합니다.
                        """)
                .build();
    }

    public RagAnswerResponse answer(String question, Integer topK, String category, Map<String, Object> meta) {
        List<Document> documents = retrieve(question, topK, category);
        if (documents.isEmpty()) {
            return new RagAnswerResponse(
                    question,
                    "검색 가능한 문서에서 관련 근거를 찾지 못했습니다. 질문을 더 구체적으로 바꾸거나 문서를 추가해 주세요.",
                    List.of(),
                    meta);
        }

        String context = documents.stream()
                .map(document -> """
                        [source=%s, category=%s, score=%.4f]
                        %s
                        """.formatted(
                        document.getMetadata().get("source"),
                        document.getMetadata().get("category"),
                        document.getScore(),
                        document.getText()))
                .reduce((left, right) -> left + "\n---\n" + right)
                .orElse("");

        String answer = chatClient.prompt()
                .user("""
                        [검색 문서]
                        %s

                        [질문]
                        %s
                        """.formatted(context, question))
                .call()
                .content();

        return new RagAnswerResponse(
                question,
                answer,
                documents.stream().map(this::toAnswerSource).toList(),
                meta);
    }

    private List<Document> retrieve(String query, Integer topK, String category) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(normalizeTopK(topK))
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .filter(document -> "true".equals(String.valueOf(document.getMetadata().get("isActive"))))
                .filter(document -> matchesCategory(document, category))
                .toList();
    }

    private RagAnswerSource toAnswerSource(Document document) {
        return new RagAnswerSource(
                String.valueOf(document.getMetadata().get("source")),
                String.valueOf(document.getMetadata().get("category")),
                document.getScore(),
                summarize(document.getText(), 360));
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

    public record RagAnswerResponse(
            String question,
            String answer,
            List<RagAnswerSource> sources,
            Map<String, Object> meta) {
    }

    public record RagAnswerSource(
            String source,
            String category,
            double score,
            String text) {
    }
}
