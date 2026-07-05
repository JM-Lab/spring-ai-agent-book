package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter2.advisor.ElapsedTimeAdvisor;

/**
 * 2장 Step 5: 어드바이저 체인 (Advisor Chain, 스트리밍).
 *
 * 학습 포인트:
 *  - SimpleLoggerAdvisor: 내장 어드바이저, 요청/응답을 DEBUG 로그로 기록
 *  - ElapsedTimeAdvisor: 커스텀 어드바이저, 응답 시간 측정 (CallAdvisor + StreamAdvisor)
 *  - 여러 어드바이저를 체인으로 구성: AOP 스타일의 관심사 분리
 *
 * 스트리밍 응답 흐름 위에서 {@link ElapsedTimeAdvisor#adviseStream} 이 정상 동작하는지,
 * {@link SimpleLoggerAdvisor} 가 리액티브 스레드에서 요청/응답을 제대로 로깅하는지
 * 함께 확인한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch2-step5")
class Ch2Step5_Advisor implements CommandLineRunner {

    private final ChatClient chatClient;

    Ch2Step5_Advisor(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultAdvisors(
                        // 내장: 요청과 응답 DEBUG 로그
                        new SimpleLoggerAdvisor(),
                        // 커스텀: 응답 시간 측정
                        new ElapsedTimeAdvisor()
                )
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch2] Step5: 어드바이저 체인 (Logger + ElapsedTime, stream) ===");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                System.out.print("AI: ");
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
