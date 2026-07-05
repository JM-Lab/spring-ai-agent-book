package kr.jmlab.spring.ai.agent.book.chapter2;

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
 * 2장 Step 러너 시뮬레이션 통합 테스트(IT).
 *
 * <p>실제 Ollama(qwen3.5:4b) 서버가 필요하므로 일반 {@code ./mvnw test}(서파이어)에서는
 * 실행되지 않고, {@code ./mvnw verify} 또는 {@code ./mvnw failsafe:integration-test}
 * (페일세이프)로만 실행된다.</p>
 *
 * <p>각 테스트 메서드는 {@link SpringApplication#run}으로 애플리케이션을 기동하고
 * {@code --spring.ai.cli.step=ch2-stepN} 옵션으로 원하는 Step 러너만 활성화한다.
 * 표준입력은 {@link ScriptedConsole}이 대신한다. 예시 질문들을
 * 여러 턴에 걸쳐 사람이 타이핑하듯 일정 간격(기본 400ms,
 * {@code -Dchapter2.cli.input-delay-ms=...}로 조절)으로 자동 입력하고, 마지막에
 * {@code /exit}로 종료한다. 스크립트 소진 후에도 {@code /exit}를 반복 공급하므로
 * 어떤 경우에도 입력 대기로 빌드가 멈추지 않는다.</p>
 *
 * <p>콘솔에는 스프링 부트 기동 로그와 함께 스트리밍 방식의 AI 응답이 그대로 찍힌다.
 * 이 출력 중 AI 응답 부분만 발췌해 책의 예제로 소개한다. 실제 개발 시에는
 * {@code ./mvnw spring-boot:run}으로 대화형 CLI를 사용한다.</p>
 */
class Chapter2StepRunnerIT {

    private InputStream originalIn;

    @BeforeEach
    void saveOriginalStdin() {
        this.originalIn = System.in;
    }

    @AfterEach
    void restoreStdin() {
        System.setIn(this.originalIn);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-step1, 기본 스트림 채팅 (2턴)")
    void step1_basicStreamChat() {
        runWithStep("ch2-step1",
                "자바 21의 주요 신기능을 3가지만 한 줄씩 짧게 알려주세요.",
                "Spring AI 를 한 줄로 짧게 소개해 주세요.");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-step2, 페르소나 (시니어 자바 멘토, 2턴)")
    void step2_페르소나() {
        runWithStep("ch2-step2",
                "Optional 은 언제 쓰면 좋나요? 짧게 알려주세요.",
                "자바 21의 새로운 기능 3가지만 알려줘.");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-step3, 구조화한 출력 (Recipe 매핑)")
    void step3_구조화한출력() {
        runWithStep("ch2-step3", "김치찌개");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-step4, 대화 메모리 (다중 턴)")
    void step4_메모리() {
        runWithStep("ch2-step4",
                "내 이름은 홍길동이야. 기억해 줘.",
                "내 이름이 뭐라고 했지?");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-step5, 어드바이저 체인 (Logger + ElapsedTime, 2턴)")
    void step5_어드바이저() {
        runWithStep("ch2-step5",
                "Spring AI 를 한 줄로 짧게 소개해 주세요.",
                "ChatClient 와 ChatModel 의 차이를 한 문장으로 알려줘.");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch2-final, 전체 통합 (Persona + Memory + Advisor + Streaming, 2턴)")
    void final_통합() {
        runWithStep("ch2-final",
                "나는 자바 10년차 개발자야.",
                "방금 내가 말한 내 경력을 반영해서, Spring AI ChatClient 의 장점을 2문장으로 알려줘.");
    }

    // ---------------------------------------------------------------------

    /**
     * 준비한 질문을 순서대로 입력한 뒤 /exit 로 종료되는 Step 러너 실행을 시뮬레이션한다.
     */
    private void runWithStep(String step, String... userInputs) {
        // ScriptedConsole: 표준입력 시뮬레이션, CapturingConsole: 콘솔 + 라이브 캡처 파일
        try (ScriptedConsole console = ScriptedConsole.withDefaultDelay(userInputs);
                CapturingConsole capture = CapturingConsole.forStep(step)) {
            System.setIn(console.start());
            String[] args = {"--spring.ai.cli.step=" + step};
            try (ConfigurableApplicationContext ctx =
                    SpringApplication.run(Chapter2Application.class, args)) {
                // CommandLineRunner 는 run() 반환 전에 이미 실행되었다.
            }
        }
    }
}
