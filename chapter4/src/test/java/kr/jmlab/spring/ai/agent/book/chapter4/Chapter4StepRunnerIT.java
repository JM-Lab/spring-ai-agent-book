package kr.jmlab.spring.ai.agent.book.chapter4;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 4장 Step 러너 시뮬레이션 통합 테스트(IT).
 *
 * <p>책 4장 "실행 후 결과" 예제에 등장하는 질문을 {@link CliInputSimulator} 로 자동 입력해
 * 각 스텝의 여러 턴 대화 흐름을 재현한다. 로컬 Ollama(qwen3.5:4b)가 필요하므로
 * 서파이어({@code ./mvnw test})에서는 제외되고 failsafe 단계에서만 실행된다:
 * {@code ./mvnw verify} 또는
 * {@code ./mvnw test-compile failsafe:integration-test -Dit.test='Chapter4StepRunnerIT#step1_methodTools'}</p>
 */
class Chapter4StepRunnerIT {

    private InputStream originalStandardIn;

    @BeforeEach
    void saveOriginalStandardIn() {
        this.originalStandardIn = System.in;
    }

    @AfterEach
    void restoreStandardIn() {
        System.setIn(this.originalStandardIn);
    }

    @Test
    @DisplayName("ch4-step1, @Tool 메서드 툴: 시간/계산 질문 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step1_methodTools() {
        runStepWithUserInputs("ch4-step1",
                "지금 서울 시간 알려줘",
                "128 곱하기 32 계산해줘");
    }

    @Test
    @DisplayName("ch4-step2, FunctionToolCallback: 할인/연락처 질문 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step2_functionTools() {
        runStepWithUserInputs("ch4-step2",
                "35000원 상품을 18% 할인하면 얼마야?",
                "고객 C-100 연락처 확인해줘");
    }

    @Test
    @DisplayName("ch4-step3, ToolContext + returnDirect: 세션 요약 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step3_toolContext() {
        runStepWithUserInputs("ch4-step3",
                "지금 학습 주제를 Tool Calling으로 두고 세션 요약해줘",
                "학습 주제를 ToolContext로 바꿔서 다시 세션 요약해줘");
    }

    @Test
    @DisplayName("ch4-step4, ToolCallingManager 수동 루프: 계산/재고 질문 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step4_toolCallingManager() {
        runStepWithUserInputs("ch4-step4",
                "144 나누기 12 결과 알려줘",
                "SKU-100 재고 확인하고 2개 예약할 수 있는지 알려줘");
    }

    @Test
    @DisplayName("ch4-step5, ToolCallingAdvisor: 날짜/재고/할 일 조합 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step5_toolCallingAdvisor() {
        runStepWithUserInputs("ch4-step5",
                "오늘 날짜와 SKU-200 재고를 확인한 뒤 할 일로 기록해줘",
                "지금까지 할 일 목록 보여줘");
    }

    @Test
    @DisplayName("ch4-final, Tool CLI 통합: 재고 조회/예약/할 일 확인 흐름 시뮬레이션")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void final_toolCliChatbot() {
        runStepWithUserInputs("ch4-final",
                "상품 목록 보여줘",
                "SKU-100 재고 2개 예약해줘",
                "지금까지 할 일 목록 보여줘");
    }

    /**
     * 준비한 질문을 순서대로 입력한 뒤 /exit 로 종료되는 Step 러너 실행을 시뮬레이션한다.
     */
    private void runStepWithUserInputs(String step, String... userInputs) {
        // CliInputSimulator: 표준입력 시뮬레이션, CapturingConsole: 콘솔 + 라이브 캡처 파일
        try (CliInputSimulator inputSimulator = CliInputSimulator.withDefaultDelay(userInputs);
             CapturingConsole capture = CapturingConsole.forStep(step)) {
            System.setIn(inputSimulator.start());
            String[] arguments = {"--spring.ai.cli.step=" + step};
            try (ConfigurableApplicationContext applicationContext =
                         SpringApplication.run(Chapter4Application.class, arguments)) {
                // CommandLineRunner 는 SpringApplication.run() 반환 전에 이미 실행되었다.
            }
        }
    }
}
