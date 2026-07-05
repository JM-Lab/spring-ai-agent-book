package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.AdvancedRagService;

/**
 * 3장 Step 5: Advanced RAG 구성 요소 확인.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step5")
class Ch3Step5_AdvancedRagModules implements CommandLineRunner {

    private final AdvancedRagService ragService;

    Ch3Step5_AdvancedRagModules(AdvancedRagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step5: Advanced RAG Modules ===\n");

        AdvancedRagService.RagPipeline pipeline = ragService.pipeline();
        printPipeline(pipeline);

        String question = "긴급 장애 대응 문서는 어떤 정보를 기록해야 하나요?";
        System.out.println("\n[DocumentRetriever + DocumentPostProcessor]");
        System.out.println("질문: " + question);

        List<Document> documents = ragService.retrieve(question);
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
            System.out.println("   " + summarize(document.getText(), 180));
        }
    }

    private void printPipeline(AdvancedRagService.RagPipeline pipeline) {
        System.out.println("[QueryTransformer]");
        System.out.println("- " + pipeline.queryTransformer());

        System.out.println("\n[DocumentRetriever]");
        System.out.println("- " + pipeline.documentRetriever());

        System.out.println("\n[DocumentPostProcessor]");
        pipeline.documentPostProcessors()
                .forEach(processor -> System.out.println("- " + processor));

        System.out.println("\n[QueryAugmenter]");
        System.out.println("- " + pipeline.queryAugmenter());

        System.out.println("\n[Advisor]");
        System.out.println("- " + pipeline.advisor());
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
