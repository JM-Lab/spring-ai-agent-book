package kr.jmlab.spring.ai.agent.book.chapter5;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 대화형 CLI 스텝의 표준 입력을 시뮬레이션하는 테스트 헬퍼.
 *
 * <p>Piped 스트림에 별도 작성 스레드가 준비된 대사(줄)를 일정 간격으로 흘려보낸다.
 * 준비한 대사를 모두 소진한 뒤에는 어떤 추가 읽기 요청에도 /exit 를 반복 공급해,
 * CLI 가 stdin 을 기다리며 무한 대기하는 일이 없도록 보장한다.</p>
 *
 * <ul>
 *   <li>줄 사이 간격: 시스템 프로퍼티 {@code chapter5.simulation.line-delay-millis}
 *       (기본값 400ms)</li>
 *   <li>입력 에코: 각 대사를 공급하기 직전에 System.out 에 그대로 출력한다(기본 켬).
 *       {@code -Dchapter5.simulation.echo=false} 로 끌 수 있다. 에코 덕분에 캡처 파일과
 *       콘솔에서 /resource, /answer-direct 같은 사용자 입력도 함께 보인다.</li>
 * </ul>
 */
final class SimulatedConsoleInput implements AutoCloseable {

    static final String LINE_DELAY_PROPERTY = "chapter5.simulation.line-delay-millis";

    static final String ECHO_PROPERTY = "chapter5.simulation.echo";

    private static final String EXIT_COMMAND = "/exit";
    private static final long DEFAULT_LINE_DELAY_MILLIS = 400L;
    private static final int PIPE_BUFFER_SIZE = 64 * 1024;

    private final PipedInputStream inputStream;
    private final Thread writerThread;

    private SimulatedConsoleInput(List<String> scriptLines, long lineDelayMillis, boolean echoEnabled) {
        PipedOutputStream outputStream = new PipedOutputStream();
        try {
            this.inputStream = new PipedInputStream(outputStream, PIPE_BUFFER_SIZE);
        }
        catch (IOException e) {
            throw new IllegalStateException("시뮬레이션 입력 파이프를 생성하지 못했습니다.", e);
        }
        this.writerThread = new Thread(() -> {
            try {
                for (String line : scriptLines) {
                    Thread.sleep(lineDelayMillis); // 실제 타이핑처럼 줄 사이 페이싱(선지연)
                    if (echoEnabled) {
                        // 공급 직전에 에코: 캡처(Tee)가 켜져 있으면 파일에도 함께 남는다.
                        System.out.println(line);
                    }
                    writeLine(outputStream, line);
                }
                // 대사 소진 후에는 어떤 추가 프롬프트에도 종료 명령으로 응답한다.
                // (안전장치 공급분은 캡처가 반복 문구로 오염되지 않도록 에코하지 않는다.)
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(lineDelayMillis);
                    writeLine(outputStream, EXIT_COMMAND);
                }
            }
            catch (IOException | InterruptedException expectedOnShutdown) {
                // 읽는 쪽(CLI)이 먼저 종료되어 파이프가 닫히거나 테스트가 정리(interrupt)되면
                // 정상 종료 경로이므로 무시한다.
            }
        }, "chapter5-simulated-stdin");
        this.writerThread.setDaemon(true);
    }

    /**
     * 주어진 대사로 시뮬레이션 입력을 만든다. 마지막 줄이 /exit 가 아니면 자동으로 덧붙인다.
     */
    static SimulatedConsoleInput ofLines(String... lines) {
        long lineDelayMillis = Long.getLong(LINE_DELAY_PROPERTY, DEFAULT_LINE_DELAY_MILLIS);
        boolean echoEnabled = Boolean.parseBoolean(System.getProperty(ECHO_PROPERTY, "true"));
        List<String> scriptLines = new ArrayList<>(List.of(lines));
        if (scriptLines.isEmpty() || !EXIT_COMMAND.equalsIgnoreCase(scriptLines.get(scriptLines.size() - 1).trim())) {
            scriptLines.add(EXIT_COMMAND);
        }
        return new SimulatedConsoleInput(scriptLines, lineDelayMillis, echoEnabled);
    }

    /**
     * 작성 스레드를 시작하고 System.in 에 연결할 입력 스트림을 반환한다.
     */
    InputStream start() {
        this.writerThread.start();
        return this.inputStream;
    }

    private static void writeLine(PipedOutputStream outputStream, String line) throws IOException {
        outputStream.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    @Override
    public void close() {
        this.writerThread.interrupt();
        try {
            this.inputStream.close();
        }
        catch (IOException ignored) {
            // 테스트 정리 단계의 close 실패는 무시한다.
        }
    }
}
