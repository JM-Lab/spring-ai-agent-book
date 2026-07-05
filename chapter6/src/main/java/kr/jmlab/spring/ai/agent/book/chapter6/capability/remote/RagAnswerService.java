package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

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
 * <p>MCP 서버의 {@code rag_answer_question} 툴이 호출되면, 이 서비스가 RAG 문서를 검색한 뒤
 * 서버 내부 ChatClient(= 하위 에이전트)로 답변까지 만들어 클라이언트에 완성된 답변을 돌려준다.
 * 클라이언트가 보기에는 외부 툴 한 번 호출이지만, 그 경계 너머에서 또 하나의 에이전트 루프가 도는
 * agent-as-a-tool 패턴이다.</p>
 */
@Service
@Profile("knowledge")
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
                        - [검색 문서]가 있으면 그 문서만 근거로 한국어로 답합니다.
                        - 검색 문서가 없으면 추측하지 말고 알 수 없다고 답합니다.
                        - 답변 끝에는 사용한 근거(문서 출처)를 짧게 정리합니다.
                        """)
                .build();
    }

    public RagAnswerResponse answer(String question, Integer topK, String category, Map<String, Object> meta) {
        List<Document> documents = retrieve(question, topK, category);

        // 검색 문서가 없으면 그 사실을 컨텍스트로 넘겨, 모델이 추측 대신 알 수 없다고 답하게 한다.
        String context = documents.isEmpty()
                ? "(검색된 문서 없음)"
                : documents.stream()
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
