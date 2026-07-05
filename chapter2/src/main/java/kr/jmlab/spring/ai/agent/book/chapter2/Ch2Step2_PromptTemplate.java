package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 2장 Step 2: 시스템 프롬프트와 역할(Role) 부여 (스트리밍).
 *
 * 학습 포인트:
 *  - defaultSystem() 으로 페르소나 고정
 *  - 같은 질문이어도 시스템 프롬프트에 따라 답변 톤/관점이 달라짐
 *
 * Step 1 과 동일하게 응답은 {@code .stream()} 으로 받아 실시간 출력한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch2-step2")
class Ch2Step2_PromptTemplate implements CommandLineRunner {

    private static final String SYSTEM_PROMPT = """
            당신은 시니어 자바 개발자이자 기술 멘토입니다.
            - 코드 예제는 Java 21 이상의 최신 문법을 활용합니다.
            - 답변은 단계적이고 근거를 함께 설명합니다.
            - 확실하지 않은 내용은 "모른다"고 솔직하게 답합니다.
            """;

    private final ChatClient chatClient;

    Ch2Step2_PromptTemplate(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)                 // 이 클라이언트 모든 호출에 자동 부착
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch2] Step2: 시스템 프롬프트 (시니어 자바 멘토, stream) ===");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                System.out.print("멘토: ");
                chatClient.prompt()
                        .user(input)
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }
}
