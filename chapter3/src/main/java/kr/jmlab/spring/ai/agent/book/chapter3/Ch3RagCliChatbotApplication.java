package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.AdvancedRagService;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase.IndexReport;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase.SourceDocument;

/**
 * 3장 최종 프로젝트: RAG AI Chatbot CLI.
 */
@Component
@ConditionalOnProperty(
        prefix = "spring.ai.cli",
        name = "step",
        havingValue = "ch3-final",
        matchIfMissing = true)
class Ch3RagCliChatbotApplication implements CommandLineRunner {

    private final AdvancedRagService ragService;
    private final String sessionId = UUID.randomUUID().toString();

    Ch3RagCliChatbotApplication(AdvancedRagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public void run(String... args) {
        printBanner();
        IndexReport indexReport = ragService.prepareKnowledgeBase();
        printOfflineIndex(indexReport);

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\n질문을 입력하세요. 종료하려면 /exit 또는 /quit을 입력합니다.\n");
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if (input == null || input.isBlank()) {
                    continue;
                }
                if ("/exit".equalsIgnoreCase(input.trim()) || "/quit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                printRetrievedDocuments(input);

                System.out.println("\n[최종 답변]");
                System.out.print("AI: ");
                ragService.stream(input)
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }

    private void printBanner() {
        System.out.println("""

                ══════════════════════════════════════════════════════
                  Spring AI RAG CLI Chatbot  (Chapter 3, Final)
                  Document ETL, VectorStore, Advanced RAG
                  Chat: qwen3.5:4b / Embedding: bge-m3 (Ollama)
                  종료: /exit  또는  /quit
                ══════════════════════════════════════════════════════

                Session ID: %s
                """.formatted(sessionId));
    }

    private void printOfflineIndex(IndexReport report) {
        System.out.println("[오프라인 문서 준비]");
        System.out.println("문서 디렉터리: " + report.documentsDirectory());
        System.out.println("읽은 파일:");
        for (SourceDocument sourceDocument : report.sourceDocuments()) {
            System.out.printf("- %s (type=%s, documents=%d)%n",
                    sourceDocument.filename(),
                    sourceDocument.sourceType(),
                    sourceDocument.documentCount());
        }
        System.out.printf("원천 문서: %d, 마스킹 후 문서: %d, 검색 청크: %d%n",
                report.rawDocumentCount(),
                report.maskedDocumentCount(),
                report.chunkCount());
        System.out.println("VectorStore 인덱싱 완료");
    }

    private void printRetrievedDocuments(String question) {
        // 화면에 표시할 검색 결과는 AdvancedRagService#retrieve()를 호출해 포매팅
        List<Document> documents = ragService.retrieve(question);

        System.out.println("\n[검색된 문서]");
        if (documents.isEmpty()) {
            System.out.println("- 검색된 문서가 없습니다.");
            return;
        }

        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();

            System.out.printf("%d. score=%s, source=%s, category=%s, type=%s, chunk=%s%n",
                    i + 1,
                    formatScore(document.getScore()),
                    metadata.getOrDefault("source", "-"),
                    metadata.getOrDefault("category", "-"),
                    metadata.getOrDefault("sourceType", "-"),
                    metadata.getOrDefault("chunk_index", "-"));
            System.out.println("   " + summarize(document.getText(), 220));
        }
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "n/a";
        }
        return "%.4f".formatted(score);
    }

    private String summarize(String text, int maxLength) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
