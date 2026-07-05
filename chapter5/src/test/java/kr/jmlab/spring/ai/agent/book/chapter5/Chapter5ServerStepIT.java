package kr.jmlab.spring.ai.agent.book.chapter5;

import java.io.ByteArrayInputStream;
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
 * 5장 Server Step 러너 시뮬레이션 통합 테스트(IT).
 *
 * <p>각 테스트는 server 프로파일로 Spring 컨텍스트를 짧게 띄워 Step 러너가
 * 콘솔에 출력하는 내용을 생성한다.</p>
 *
 * <p>실행 시 Ollama 의 bge-m3 임베딩 모델이 필요하다(인덱싱 단계에서 임베딩을 호출).
 * 라이브 모델을 쓰는 통합 테스트이므로 maven-failsafe-plugin 이 *IT 규칙으로만 실행하며,
 * ./mvnw test(surefire)에서는 실행되지 않는다.</p>
 */
class Chapter5ServerStepIT {

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
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-server-step1, RAG 문서 ETL과 벡터 인덱싱")
    void serverStep1_ragKnowledgeBase() {
        runServerStep("ch5-server-step1");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-server-step2, 검색 전용 패턴 MCP 툴 공개")
    void serverStep2_searchTools() {
        runServerStep("ch5-server-step2");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-server-step3, 서버 측 답변 패턴 MCP 툴 공개")
    void serverStep3_answerTools() {
        runServerStep("ch5-server-step3");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-server-step4, MCP Resource, Prompt, Completion 공개")
    void serverStep4_mcpPrimitives() {
        runServerStep("ch5-server-step4");
    }

    private void runServerStep(String step) {
        // 서버 Step 러너는 stdin 을 읽지 않으므로 빈 입력이면 충분하다.
        System.setIn(new ByteArrayInputStream("".getBytes()));
        String[] args = {
                "--spring.profiles.active=server",
                "--spring.ai.cli.step=" + step,
                "--server.port=0"
        };
        // 서버 로그와 툴 공개 출력을 콘솔 + 라이브 캡처 파일에 동시 기록
        try (CapturingConsole capture = CapturingConsole.forStep(step)) {
            try (ConfigurableApplicationContext ctx =
                         SpringApplication.run(Chapter5Application.class, args)) {
                // CommandLineRunner 는 컨텍스트 시작 도중 이미 실행되었다.
            }
        }
    }
}
