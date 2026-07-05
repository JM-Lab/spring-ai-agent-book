package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter2.advisor.ElapsedTimeAdvisor;

/**
 * 2장 최종 프로젝트: 모든 기능 통합 (스트리밍).
 *
 * 페르소나 (System Prompt)
 * + 대화 메모리 (MessageChatMemoryAdvisor)
 * + 어드바이저 체인 (SimpleLogger + ElapsedTime)
 * + 스트리밍 응답
 *
 * 앤트로픽의 "확장된 LLM (The Augmented LLM)" 개념을
 * 로컬 qwen3.5:4b 모델로 재현한 CLI 챗봇.
 */
@Component
@ConditionalOnProperty(
        prefix = "spring.ai.cli",
        name = "step",
        havingValue = "ch2-final",
        matchIfMissing = true)
class Ch2CliChatbotApplication implements CommandLineRunner {

    private static final String SYSTEM_PROMPT = """
            당신은 Spring AI 학습을 돕는 친절한 AI 어시스턴트입니다.
            다음 원칙을 지킵니다:
            - 자바와 스프링 기술에 정통하며, 예제 코드는 Java 21 이상 문법으로 설명합니다.
            - 이전 대화를 기억하고 맥락에 맞게 답변합니다.
            - 답변은 간결하고 명확하게, 과장 없이 사실 위주로 작성합니다.
            - 확실하지 않은 정보는 솔직하게 "모른다"고 답합니다.
            """;

    private final ChatClient chatClient;
    private final String conversationId = UUID.randomUUID().toString();

    Ch2CliChatbotApplication(ChatClient.Builder builder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = builder
                // 페르소나
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 기억
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 로깅
                        new SimpleLoggerAdvisor(),
                        // 응답 시간
                        new ElapsedTimeAdvisor()
                )
                .build();
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
                    System.out.println("👋 대화를 종료합니다.");
                    break;
                }

                System.out.print("AI: ");
                chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        // Step 1 부터 유지한 스트리밍 흐름
                        .stream()
                        .content()
                        // 토큰이 도착할 때마다 즉시 출력
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }

    private void printBanner() {
        System.out.println("""

                ══════════════════════════════════════════════════════
                  Spring AI CLI Chatbot  (Chapter 2, Final)
                  Persona, Memory, Advisor, Streaming
                  Model: qwen3.5:4b (via Ollama @ localhost:11434)
                  종료: /exit  또는  /quit
                ══════════════════════════════════════════════════════

                Conversation ID: %s
                """.formatted(conversationId));
    }
}
