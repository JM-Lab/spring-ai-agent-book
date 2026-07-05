package kr.jmlab.spring.ai.agent.book.chapter5;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 5장 Client Step 러너 시뮬레이션 통합 테스트(IT).
 *
 * <p>@BeforeAll 에서 server 프로파일로 MCP 서버를 띄우고, 각 테스트가 client Step 러너를
 * 실행해 콘솔 출력을 만든다.</p>
 *
 * <p>대화형인 ch5-final 은 {@link SimulatedConsoleInput}으로 예시 질문을
 * 여러 턴 자동 입력하고 마지막에 /exit 로 종료한다. 준비한 대사 소진 후에도 /exit 를
 * 반복 공급하므로 CLI 가 stdin 을 기다리며 멈추지 않는다.</p>
 *
 * <p>실행 시 Ollama 의 qwen3.5:4b 채팅 모델과 bge-m3 임베딩 모델이 모두 필요하다.
 * 라이브 모델을 쓰는 통합 테스트이므로 maven-failsafe-plugin 이 *IT 규칙으로만 실행하며,
 * ./mvnw test(surefire)에서는 실행되지 않는다.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Chapter5ClientStepIT {

    private static ConfigurableApplicationContext serverContext;
    private static String serverMcpUrl;

    private InputStream originalIn;

    @BeforeAll
    void startMcpServer() {
        String[] serverArgs = {
                "--spring.profiles.active=server",
                "--server.port=0"
        };
        serverContext = SpringApplication.run(Chapter5Application.class, serverArgs);
        String port = serverContext.getEnvironment().getProperty("local.server.port");
        serverMcpUrl = "http://localhost:" + port;
        System.out.println("[IT] MCP server up at " + serverMcpUrl);
    }

    @AfterAll
    void stopMcpServer() {
        if (serverContext != null) {
            serverContext.close();
        }
    }

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
    @DisplayName("ch5-client-step1, MCP 서버 발견")
    void clientStep1_discovery() {
        runNonInteractiveStep("ch5-client-step1");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-client-step2, MCP 프리미티브 직접 호출")
    void clientStep2_primitiveCalls() {
        runNonInteractiveStep("ch5-client-step2");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-client-step3, ToolCallback 변환")
    void clientStep3_toolCallbackProvider() {
        runNonInteractiveStep("ch5-client-step3");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-client-step4, 툴 필터링과 _meta 정책")
    void clientStep4_policy() {
        runNonInteractiveStep("ch5-client-step4");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @DisplayName("ch5-final, MCP 기반 AI Chatbot CLI 대화 시뮬레이션")
    void final_mcpCliChatbot() {
        // 예시 질문으로 여러 턴을 시뮬레이션한다.
        // 1) 클라이언트 측 답변 패턴(툴 호출 + 클라이언트 LLM)
        // 2) 슬래시 명령으로 리소스 직접 조회
        // 3) 서버 측 답변 패턴(/answer-direct → rag_answer_question)
        try (SimulatedConsoleInput simulatedInput = SimulatedConsoleInput.ofLines(
                "RAG 운영 정책은 무엇인가요?",
                "/resource rag://sources",
                "/answer-direct 긴급 장애 대응 정책은 무엇인가요?")) {
            System.setIn(simulatedInput.start());
            runClientStep("ch5-final");
        }
    }

    private void runNonInteractiveStep(String step) {
        // Step 1~4 러너는 stdin 을 읽지 않으므로 빈 입력이면 충분하다.
        System.setIn(new ByteArrayInputStream("".getBytes()));
        runClientStep(step);
    }

    private void runClientStep(String step) {
        String[] args = {
                "--spring.ai.cli.step=" + step,
                "--spring.ai.mcp.client.streamable-http.connections.rag.url=" + serverMcpUrl
        };
        // 입력 에코와 AI 응답을 콘솔 + 라이브 캡처 파일에 동시 기록
        try (CapturingConsole capture = CapturingConsole.forStep(step)) {
            try (ConfigurableApplicationContext ctx =
                         SpringApplication.run(Chapter5Application.class, args)) {
                // CommandLineRunner 는 컨텍스트 시작 도중 이미 실행되었다.
            }
        }
    }
}
