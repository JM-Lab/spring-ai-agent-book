package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Scanner;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.support.ToolEnabledChatService;

/**
 * 4장 최종 프로젝트: Tool 지원 AI Chatbot CLI.
 */
@Component
@ConditionalOnProperty(
        prefix = "spring.ai.cli",
        name = "step",
        havingValue = "ch4-final",
        matchIfMissing = true)
class Ch4ToolCliChatbotApplication implements CommandLineRunner {

    private final ToolEnabledChatService chatService;
    private final String conversationId = UUID.randomUUID().toString();

    Ch4ToolCliChatbotApplication(ToolEnabledChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void run(String... args) {
        printBanner();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if (input == null || input.isBlank()) {
                    continue;
                }
                String trimmed = input.trim();
                if ("/exit".equalsIgnoreCase(trimmed) || "/quit".equalsIgnoreCase(trimmed)) {
                    System.out.println("대화를 종료합니다.");
                    break;
                }

                System.out.print("AI: ");
                chatService.stream(input, conversationId)
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }

    private void printBanner() {
        System.out.println("""

                ══════════════════════════════════════════════════════
                  Spring AI Tool Calling CLI  (Chapter 4, Final)
                  @Tool, FunctionToolCallback, ToolCallingAdvisor
                  Model: qwen3.5:4b (via Ollama @ localhost:11434)
                  종료: /exit  또는  /quit
                ══════════════════════════════════════════════════════

                Conversation ID: %s
                """.formatted(conversationId));
    }
}
