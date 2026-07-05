package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.CalculatorTools;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.Chapter4ToolCallbacks;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.DateTimeTools;
import kr.jmlab.spring.ai.agent.book.chapter4.support.InventoryTools;
import kr.jmlab.spring.ai.agent.book.chapter4.support.TodoTools;

/**
 * 4장 Step 5: ToolCallingAdvisor 로 ChatClient 툴 실행을 어드바이저 체인에 통합한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch4-step5")
class Ch4Step5_ToolCallingAdvisor implements CommandLineRunner {

    private final ChatClient chatClient;
    private final String conversationId = UUID.randomUUID().toString();

    Ch4Step5_ToolCallingAdvisor(
            ChatClient.Builder builder,
            DateTimeTools dateTimeTools,
            CalculatorTools calculatorTools,
            TodoTools todoTools,
            InventoryTools inventoryTools) {
        this.chatClient = builder.clone()
                .defaultSystem("""
                        당신은 툴 실행 과정을 투명하게 보여 주는 AI 어시스턴트입니다.
                        필요한 경우 날짜, 계산, 할인, 고객 연락처, 할 일, 상품 재고 툴을 사용합니다.
                        """)
                .defaultTools(dateTimeTools, calculatorTools, todoTools, inventoryTools)
                .defaultTools(
                        Chapter4ToolCallbacks.discountCalculator(),
                        Chapter4ToolCallbacks.customerContactLookup(),
                        Chapter4ToolCallbacks.sessionSummary(false)
                )
                .defaultAdvisors(
                        ToolCallingAdvisor.builder().build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch4] Step5: ToolCallingAdvisor + Advisor Chain ===");
        System.out.println("예) 오늘 날짜와 SKU-200 재고를 확인한 뒤 할 일로 기록해줘");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                System.out.print("AI: ");
                chatClient.prompt()
                        .user(input)
                        .toolContext(Map.of(
                                "userName", "chapter4-reader",
                                "conversationId", conversationId
                        ))
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }
}
