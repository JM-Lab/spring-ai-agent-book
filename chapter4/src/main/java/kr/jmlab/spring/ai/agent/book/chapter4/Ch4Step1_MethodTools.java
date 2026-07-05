package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Arrays;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.CalculatorTools;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.DateTimeTools;

/**
 * 4장 Step 1: @Tool 메서드 기반 툴 등록.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch4-step1")
class Ch4Step1_MethodTools implements CommandLineRunner {

    private final ChatClient chatClient;
    private final DateTimeTools dateTimeTools;
    private final CalculatorTools calculatorTools;

    Ch4Step1_MethodTools(
            ChatClient.Builder builder,
            DateTimeTools dateTimeTools,
            CalculatorTools calculatorTools) {
        this.dateTimeTools = dateTimeTools;
        this.calculatorTools = calculatorTools;
        this.chatClient = builder.clone()
                .defaultSystem("""
                        당신은 계산과 날짜 확인을 툴로 처리하는 AI 어시스턴트입니다.
                        툴 결과를 확인한 뒤 한국어로 짧게 답변합니다.
                        """)
                .defaultTools(dateTimeTools, calculatorTools)
                .defaultAdvisors(ToolCallingAdvisor.builder().build())
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch4] Step1: @Tool 메서드 기반 툴 ===");
        Arrays.stream(ToolCallbacks.from(dateTimeTools, calculatorTools))
                .forEach(callback -> {
                    var definition = callback.getToolDefinition();
                    System.out.printf("- %s: %s%n", definition.name(), definition.description());
                });
        System.out.println("\n예) 지금 서울 시간 알려줘 / 128 곱하기 32 계산해줘");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                String answer = chatClient.prompt()
                        .user(input)
                        .call()
                        .content();
                System.out.println("AI: " + answer + "\n");
            }
        }
    }
}
