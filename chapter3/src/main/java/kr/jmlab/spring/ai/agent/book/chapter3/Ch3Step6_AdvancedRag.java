package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.AdvancedRagService;

/**
 * 3장 Step 6: RetrievalAugmentationAdvisor 기반 Advanced RAG.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step6")
class Ch3Step6_AdvancedRag implements CommandLineRunner {

    private final AdvancedRagService ragService;

    Ch3Step6_AdvancedRag(AdvancedRagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step6: Advanced RAG (RetrievalAugmentationAdvisor) ===");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                System.out.print("AI: ");
                ragService.stream(input)
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }
}
