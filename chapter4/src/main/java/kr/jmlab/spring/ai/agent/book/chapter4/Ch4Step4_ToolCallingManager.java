package kr.jmlab.spring.ai.agent.book.chapter4;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.ManualToolCallingService;

/**
 * 4장 Step 4: ToolCallingManager 로 툴 실행 루프를 직접 제어한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch4-step4")
class Ch4Step4_ToolCallingManager implements CommandLineRunner {

    private final ManualToolCallingService manualToolCallingService;

    Ch4Step4_ToolCallingManager(ManualToolCallingService manualToolCallingService) {
        this.manualToolCallingService = manualToolCallingService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch4] Step4: ToolCallingManager 수동 실행 루프 ===");
        System.out.println("예) SKU-100 재고 확인하고 2개 예약할 수 있는지 알려줘");
        System.out.println("종료하려면 /exit 입력\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                String answer = manualToolCallingService.ask(input);
                System.out.println("AI: " + answer + "\n");
            }
        }
    }
}
