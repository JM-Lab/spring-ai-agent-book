package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.Chapter4ToolCallbacks;

/**
 * 4장 Step 2: FunctionToolCallback 기반 함수형 툴.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch4-step2")
class Ch4Step2_FunctionTools implements CommandLineRunner {

    private final ChatClient chatClient;
    private final ToolCallback discountCalculator;
    private final ToolCallback customerContactLookup;

    Ch4Step2_FunctionTools(ChatClient.Builder builder) {
        this.discountCalculator = Chapter4ToolCallbacks.discountCalculator();
        this.customerContactLookup = Chapter4ToolCallbacks.customerContactLookup();
        this.chatClient = builder.clone()
                .defaultSystem("""
                        당신은 가격 계산과 고객 연락처 확인을 돕는 AI 어시스턴트입니다.
                        할인 계산은 반드시 discount_calculator 툴을 사용합니다.
                        고객 연락처 조회는 반드시 customer_contact_lookup 툴을 사용합니다.
                        """)
                .defaultTools(
                        Chapter4ToolCallbacks.discountCalculator(),
                        Chapter4ToolCallbacks.customerContactLookup()
                )
                .defaultAdvisors(ToolCallingAdvisor.builder().build())
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch4] Step2: FunctionToolCallback 함수형 툴 ===");
        for (ToolCallback callback : new ToolCallback[] {discountCalculator, customerContactLookup}) {
            var definition = callback.getToolDefinition();
            System.out.printf("- %s: %s%n", definition.name(), definition.description());
        }
        System.out.println("\n예) 35000원 상품을 18% 할인하면 얼마야? / 고객 C-100 연락처 확인해줘");
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
