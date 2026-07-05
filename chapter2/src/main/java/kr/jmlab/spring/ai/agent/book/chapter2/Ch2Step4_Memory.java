package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 2장 Step 4: 대화 메모리 (다중 턴, 스트리밍).
 *
 * 학습 포인트:
 *  - MessageWindowChatMemory 로 최근 N 개 메시지를 슬라이딩 윈도우 유지
 *  - MessageChatMemoryAdvisor 를 통해 대화 내역을 프롬프트에 자동 주입
 *  - CONVERSATION_ID 파라미터로 세션 구분
 *
 * 실습 아이디어:
 *   1) "내 이름은 홍길동이야" 입력
 *   2) "내 이름이 뭐라고?" 입력 → 이전 턴 기억 여부 확인
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch2-step4")
class Ch2Step4_Memory implements CommandLineRunner {

    private final ChatClient chatClient;
    private final String conversationId = UUID.randomUUID().toString();

    Ch2Step4_Memory(ChatClient.Builder builder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                // 최근 20개 메시지 유지
                .maxMessages(20)
                .build();

        this.chatClient = builder
                // 어드바이저 체인에 대화 메모리 어드바이저를 등록
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch2] Step4: 대화 메모리 (다중 턴, stream) ===");
        System.out.println("Conversation ID: " + conversationId);
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                System.out.print("AI: ");
                chatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }
}
