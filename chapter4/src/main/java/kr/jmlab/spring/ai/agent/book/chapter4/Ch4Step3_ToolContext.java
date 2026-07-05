package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.Chapter4ToolCallbacks;

/**
 * 4장 Step 3: ToolContext 와 returnDirect 메타데이터.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch4-step3")
class Ch4Step3_ToolContext implements CommandLineRunner {

    private final ChatClient chatClient;
    private final String conversationId = UUID.randomUUID().toString();

    Ch4Step3_ToolContext(ChatClient.Builder builder) {
        this.chatClient = builder.clone()
                .defaultSystem("""
                        당신은 CLI 세션 정보를 요약하는 AI 어시스턴트입니다.
                        사용자가 세션 요약을 요청하면 session_summary 툴을 호출합니다.
                        """)
                .defaultTools(Chapter4ToolCallbacks.sessionSummary(true))
                .defaultAdvisors(ToolCallingAdvisor.builder().build())
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch4] Step3: ToolContext + returnDirect ===");
        System.out.println("예) 지금 학습 주제를 Tool Calling으로 두고 세션 요약해줘");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                // 요청 단위로 ToolContext 전달
                String answer = chatClient.prompt()
                        .user(input)
                        .toolContext(Map.of(
                                "userName", "chapter4-reader",
                                "conversationId", conversationId
                        ))
                        .call()
                        .content();
                System.out.println("AI: " + answer + "\n");
            }
        }
    }
}
