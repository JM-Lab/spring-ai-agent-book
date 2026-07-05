package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 2장 Step 1: 스트리밍 기반 기본 채팅 (Basic Stream Chat).
 *
 * 학습 포인트:
 *  - ChatClient.Builder 주입 → 불변 클라이언트 생성
 *  - prompt().user().stream().content() 의 Flux 기반 API
 *  - 토큰이 생성되는 즉시 콘솔에 출력되어 “타자 치는 듯한” UX 구현
 *
 * 응답을 {@code .call()} 이 아니라 처음부터 {@code .stream()} 으로 받아, 이후 모든 Step 이
 * 실제 서비스에 가까운 스트리밍 흐름으로 일관되게 동작하도록 한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch2-step1")
class Ch2Step1_BasicStreamChat implements CommandLineRunner {

    private final ChatClient chatClient;

    Ch2Step1_BasicStreamChat(ChatClient.Builder builder) {
        // 빌더를 이용해 불변 클라이언트 생성
        this.chatClient = builder.build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch2] Step1: 기본 스트림 채팅 (ChatClient, stream) ===");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                System.out.print("AI: ");
                chatClient.prompt()
                        .user(input)
                        // 스트리밍 응답 활성화
                        .stream()
                        // 문자열 토큰 스트림으로 변환
                        .content()
                        // 토큰이 도착하는 즉시 터미널에 출력
                        .doOnNext(System.out::print)
                        // 리액터 비동기 흐름을 CLI 동기 진행 순서에 맞춤
                        .blockLast();
                System.out.println("\n");
            }
        }
    }
}
