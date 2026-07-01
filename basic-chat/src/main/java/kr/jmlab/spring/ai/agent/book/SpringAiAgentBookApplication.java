package kr.jmlab.spring.ai.agent.book;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 가장 단순한 Spring AI 콘솔 챗.
 *
 * <p>CommandLineRunner 빈이 애플리케이션 시작 직후 한 번 실행되어,
 * 사전에 정의한 프롬프트를 {@code ChatClient#call()} 로 전송하고 응답을 콘솔에 출력한다.
 * 스트리밍, 메모리, 구조화한 출력 같은 확장 없이 꾸린 최소 구성으로,
 * Spring AI 의 “질문-응답 한 왕복” 기본기를 이해하기 위한 참조 구현이다.</p>
 */
@SpringBootApplication
public class SpringAiAgentBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAgentBookApplication.class, args);
    }

    /**
     * 애플리케이션 시작 시 자동으로 실행되는 Runner 빈 등록
     * ChatClient.Builder는 스프링 AI가 자동으로 주입
     */
    @Bean
    public CommandLineRunner chatRunner(ChatClient.Builder chatClientBuilder) {
        return args -> {
            // 1. 질문 준비 (userPrompt 값을 바꿔 실행해 보세요!)
            String userPrompt = "안녕 Spring AI!";
            System.out.println(">>> AI에게 질문: " + userPrompt);

            // 2. AI에게 질문 전송 및 응답 수신
            String response = chatClientBuilder.build()
                    .prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            // 3. 최종 답변 출력
            System.out.println(">>> AI 답변: " + response);
        };
    }
}
