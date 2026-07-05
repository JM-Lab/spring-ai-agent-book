package kr.jmlab.spring.ai.agent.book.chapter6;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 6장 Client Step 러너 시뮬레이션 통합 테스트(IT).
 *
 * <p>각 테스트가 {@code --spring.ai.cli.step=ch6-stepN} 러너를 <b>실제 Ollama</b>로 실행하고,
 * 예시 사용자 입력(승인 게이트 y 응답, 통합 CLI의 대화 문장)을 <b>스크립트로 자동 입력</b>해
 * 사람 없이 전체 흐름을 재현한다. 콘솔 출력은 {@code target/ch6-capture/<step>.txt}로 캡처한다.</p>
 *
 * <p>입력 시뮬레이션: {@link PipedInputStream}을 {@code System.in}에 연결하고, 작성 스레드가 스크립트를
 * 한 줄씩 공급한다. 줄 사이 기본 딜레이는 400ms이며 {@code -Dch6.sim.line-delay-ms=...}로 조절한다.
 * 직전 줄이 소비될 때까지 다음 줄을 쓰지 않아(파이프 잔량 확인) 서로 다른 Scanner가 여러 줄을 한 번에
 * 삼키는 문제를 막고, 스크립트 소진 후에는 {@code /exit}를 반복 공급해 어떤 프롬프트가 남아도 세션이
 * 반드시 종료되게 한다.</p>
 *
 * <p>서버 운용: 지식(RAG, 읽기 전용) 서버는 인덱싱 비용이 커서 @BeforeAll에서 한 번만 띄워 공유하고,
 * 운영(주문, 재고) 서버는 발주, 예약이 인메모리 상태를 바꾸므로 <b>스텝마다 새로 띄워 재고를 초기화</b>한다
 * (책처럼 각 스텝이 깨끗한 상태에서 시작).</p>
 *
 * <p>실행 시 Ollama의 qwen3.5:4b 채팅 모델과 bge-m3 임베딩 모델이 모두 필요하다. 이름이 *IT라서
 * {@code ./mvnw test}(surefire)에서는 실행되지 않고, failsafe로만 실행된다:<br>
 * {@code ./mvnw verify} 또는 {@code ./mvnw verify -Dit.test='Chapter6ClientStepIT#step3_approvalGate'}</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Chapter6ClientStepIT {

    private static final Path CAPTURE_DIR = Path.of("target", "ch6-capture");

    /** 스크립트 줄 사이 딜레이(ms). 사람 타이핑을 흉내 내며, -Dch6.sim.line-delay-ms로 조절한다. */
    private static final long LINE_DELAY_MS = Long.getLong("ch6.sim.line-delay-ms", 400L);

    /** 입력 에코(기본 켬). 공급하는 줄을 System.out(Tee 경유 → 콘솔, 캡처)에도 찍어 대화처럼 보이게 한다. -Dch6.sim.echo=false로 끈다. */
    private static final boolean ECHO_ENABLED = Boolean.parseBoolean(System.getProperty("ch6.sim.echo", "true"));

    /** 지식 연결(application.yml에는 주석 처리됨)을 Step 4부터 인자로 활성화. */
    private static final String KNOWLEDGE_URL =
            "--spring.ai.mcp.client.streamable-http.connections.knowledge.url=http://localhost:8086";

    /** 캡처를 깨끗하게: 스프링 로그는 WARN으로 낮추고 배너를 끈다(에이전트 트레이스는 System.out이라 보존됨). */
    private static final String[] QUIET = {
            "--logging.level.root=WARN",
            "--spring.main.banner-mode=off"
    };

    private static ConfigurableApplicationContext knowledgeServer;

    @BeforeAll
    void startKnowledgeServer() throws IOException {
        Files.createDirectories(CAPTURE_DIR);
        // 지식 서버는 기동 시 문서 인덱싱 + 샘플 RAG 호출(Ollama)을 하고, 이후 읽기 전용으로 공유된다.
        knowledgeServer = bootServer("knowledge", "knowledge-server");
    }

    @AfterAll
    void stopKnowledgeServer() {
        if (knowledgeServer != null) {
            knowledgeServer.close();
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step1, 코어 메인 에이전트 (로컬 툴, 단독 실행)")
    void step1_core() {
        runClientStep("ch6-step1", List.of(), "--spring.ai.mcp.client.enabled=false");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step2, 운영 MCP 서버 연결, check_stock")
    void step2_mcpConnect() {
        runClientStep("ch6-step2", List.of());
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step3, 비가역 작업 승인 게이트 (HITL, 발주 요청 → 승인 y)")
    void step3_approvalGate() {
        // 러너가 "SKU-300을 10개 발주해줘"를 스스로 던지고, 승인 게이트가 y/n를 물으면 y로 승인한다.
        runClientStep("ch6-step3", List.of("y"));
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step4, 지식 서버 agent-as-a-tool (경로 a)")
    void step4_agentAsTool() {
        runClientStep("ch6-step4", List.of(), KNOWLEDGE_URL);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step5, 하위 에이전트 위임 TaskTool (경로 b)")
    void step5_taskTool() {
        runClientStep("ch6-step5", List.of(), KNOWLEDGE_URL);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-step6, 오케스트레이션 (스킬 절차 → 발주 승인 y)")
    void step6_orchestration() {
        // 러너가 재발주 검토를 스스로 요청하고, 스킬 절차 끝의 place_purchase_order 승인만 y로 답한다.
        runClientStep("ch6-step6", List.of("y"), KNOWLEDGE_URL);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @DisplayName("ch6-final, 통합 CLI (재고 확인 → 재발주 승인 → 보고서 위임 → 종료)")
    void finalStep_integrated() {
        // 통합 CLI 흐름 시뮬레이션: 재고 확인 → 재발주 정책 수행(승인 y) → 보고서 위임 → 종료.
        runClientStep("ch6-final",
                List.of(
                        "운영 시스템에서 SKU-200의 현재 재고를 확인해줘",
                        "사내 재발주 정책에 따라 SKU-200의 재발주가 필요한지 검토하고, 필요하면 발주까지 진행해줘",
                        "y",
                        "방금 확인한 재고와 발주 내용으로 업무 보고서 초안을 report-writer 하위 에이전트에 맡겨 작성해줘",
                        "/exit"),
                KNOWLEDGE_URL);
    }

    /** 운영 서버를 새로 띄워(재고 초기화) 한 client 스텝을 실행하고, 스크립트를 자동 입력하며 출력을 캡처한다. */
    private void runClientStep(String step, List<String> script, String... extraArgs) {
        ConfigurableApplicationContext opsServer = bootServer("ops", "ops-server");
        InputStream originalIn = System.in;

        PipedOutputStream scriptSource = new PipedOutputStream();
        PipedInputStream simulatedStdin = newPipe(scriptSource);
        Thread feeder = startScriptFeeder(scriptSource, simulatedStdin, script);

        System.setIn(simulatedStdin);   // 러너, 승인 핸들러의 Scanner가 이 파이프를 읽는다 (컨텍스트 기동 전에 교체)
        // CapturingConsole: 콘솔 + 라이브 캡처 파일(target/ch6-capture/<step>.txt). tail -f 로 실시간 스트리밍.
        try (CapturingConsole capture = CapturingConsole.forStep(step)) {
            // 지식 서버의 RAG 하위 에이전트(로컬 4b)는 답변 생성에 20초(기본 타임아웃)를 넘길 수 있어 넉넉히 늘린다.
            String[] base = {"--spring.ai.cli.step=" + step, "--spring.ai.mcp.client.request-timeout=120s"};
            String[] args = concat(concat(base, QUIET), extraArgs);
            try (ConfigurableApplicationContext ctx = SpringApplication.run(Chapter6Application.class, args)) {
                // CommandLineRunner 가 컨텍스트 시작 도중 이미 실행되었다.
            }
        } finally {
            System.setIn(originalIn);
            feeder.interrupt();
            closeQuietly(scriptSource);
            closeQuietly(simulatedStdin);
            opsServer.close();
        }
        // 자동 단언은 두지 않는다. 멀티턴 흐름의 완주와 응답 품질은 캡처(target/ch6-capture/<step>.txt)를
        // 사람이 보고 판단한다(다른 장 IT와 동일한 방식). 서버 기동, 컨텍스트 초기화 같은 하드 실패는 예외로 드러나 테스트가 깨진다.
    }

    /**
     * 스크립트 작성 스레드. 줄 사이 기본 딜레이를 두고, 직전 줄이 소비될 때까지(파이프 잔량 0) 다음 줄을
     * 쓰지 않는다. 스크립트 소진 후에는 {@code /exit}를 반복 공급해, 남은 승인 프롬프트나 대화 프롬프트가
     * 무엇이든 세션이 스스로 끝나게 한다(승인 게이트는 y 외 입력을 모두 거절로 처리하므로 안전).
     */
    private Thread startScriptFeeder(PipedOutputStream target, PipedInputStream pipe, List<String> script) {
        Thread feeder = new Thread(() -> {
            try {
                for (String line : script) {
                    waitUntilDrained(pipe);
                    if (ECHO_ENABLED) {
                        // 공급 직전 에코: System.out 이 Tee 이므로 콘솔, 캡처 파일에 "질문" 이 남는다.
                        System.out.println(line);
                    }
                    target.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                    target.flush();
                }
                while (!Thread.currentThread().isInterrupted()) {
                    waitUntilDrained(pipe);
                    target.write(("/exit" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                    target.flush();
                }
            } catch (IOException | InterruptedException ex) {
                // 파이프가 닫혔거나(스텝 종료) 인터럽트 → 조용히 종료
            }
        }, "ch6-sim-stdin-feeder");
        feeder.setDaemon(true);
        feeder.start();
        return feeder;
    }

    /** 줄 사이 딜레이를 지킨 뒤, 직전에 쓴 줄이 소비될 때까지 대기한다. */
    private static void waitUntilDrained(PipedInputStream pipe) throws IOException, InterruptedException {
        Thread.sleep(LINE_DELAY_MS);
        while (pipe.available() > 0) {
            Thread.sleep(200);
        }
    }

    private static PipedInputStream newPipe(PipedOutputStream source) {
        try {
            return new PipedInputStream(source, 8192);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 종료 정리 중 예외는 무시
        }
    }

    /** 서버 프로파일을 띄우면서 기동 출력(서버 블록)을 콘솔 + 라이브 캡처 파일로 남긴다. 컨텍스트는 살려서 반환한다. */
    private ConfigurableApplicationContext bootServer(String profile, String captureName) {
        try (CapturingConsole capture = CapturingConsole.forStep(captureName)) {
            String[] args = concat(new String[] {"--spring.profiles.active=" + profile}, QUIET);
            return SpringApplication.run(Chapter6Application.class, args);
        }
    }

    private static String[] concat(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
