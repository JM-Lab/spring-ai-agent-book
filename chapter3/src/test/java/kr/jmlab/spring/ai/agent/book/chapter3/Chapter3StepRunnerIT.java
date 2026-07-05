package kr.jmlab.spring.ai.agent.book.chapter3;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 3장 라이브 Step 러너 시뮬레이션 통합 테스트 (Ollama + 임베딩 모델 필요).
 *
 * <p>실행 조건: 로컬 Ollama(localhost:11434)에 qwen3.5:4b(채팅), bge-m3(임베딩)가
 * 준비되어 있어야 한다. maven-failsafe-plugin이 {@code *IT} 패턴으로 수집하므로
 * {@code ./mvnw test}에서는 실행되지 않고 {@code ./mvnw verify}
 * (또는 {@code ./mvnw failsafe:integration-test failsafe:verify})에서 실행된다.</p>
 *
 * <p>대화형 스텝(Step 6, final)은 예시 질문을
 * {@link SimulatedCliInput}으로 여러 턴 순서대로 자동 입력한다. 줄 사이 딜레이는
 * 기본 400ms이며 {@code -Dchapter3.sim.lineDelayMillis=...}로 조절한다.</p>
 */
class Chapter3StepRunnerIT {

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
    @DisplayName("ch3-step4, Embedding + VectorStore (라이브 인덱싱)")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step4_vectorStore() {
        runWithScript("ch3-step4", List.of());
    }

    @Test
    @DisplayName("ch3-step5, Advanced RAG modules (라이브 인덱싱)")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step5_advancedRagModules() {
        runWithScript("ch3-step5", List.of());
    }

    @Test
    @DisplayName("ch3-step6, Advanced RAG 대화 시뮬레이션 (라이브 인덱싱 + 생성)")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void step6_advancedRag() {
        // Advanced RAG 대화 질문 2턴 + 종료
        runWithScript("ch3-step6", List.of(
                "긴급 장애 대응 문서는 어떻게 검색해야 하나요?",
                "긴급 장애 대응 문서는 어떤 정보를 기록해야 하나요?",
                "/exit"
        ));
    }

    @Test
    @DisplayName("ch3-final, RAG CLI 챗봇 대화 시뮬레이션 (라이브 인덱싱 + 생성)")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void final_ragCliChatbot() {
        // 최종 대화 질문 2턴 + 종료
        runWithScript("ch3-final", List.of(
                "SimpleVectorStore는 어떤 상황에서 쓰면 좋나요?",
                "긴급 장애 대응 문서는 어떤 정보를 기록해야 하나요?",
                "/exit"
        ));
    }

    private void runWithScript(String step, List<String> scriptLines) {
        // SimulatedCliInput: 표준입력 시뮬레이션, CapturingConsole: 콘솔 + 라이브 캡처 파일
        try (SimulatedCliInput simulatedInput = new SimulatedCliInput(scriptLines);
             CapturingConsole capture = CapturingConsole.forStep(step)) {
            System.setIn(simulatedInput.stream());
            String[] args = {"--spring.ai.cli.step=" + step};
            try (ConfigurableApplicationContext ctx =
                         SpringApplication.run(Chapter3Application.class, args)) {
                // CommandLineRunner 는 run() 반환 전에 이미 실행되었다.
            }
        }
    }
}
