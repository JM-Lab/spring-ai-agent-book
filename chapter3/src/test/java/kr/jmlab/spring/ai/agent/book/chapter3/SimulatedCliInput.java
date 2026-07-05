package kr.jmlab.spring.ai.agent.book.chapter3;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 책 예제 시나리오를 사람처럼 순서대로 입력하는 시뮬레이션 표준 입력.
 *
 * <p>PipedInputStream + 작성 스레드로 동작한다. 각 줄 사이에 딜레이(기본 400ms,
 * 시스템 프로퍼티 {@code chapter3.sim.lineDelayMillis}로 조절)를 두어 실제 사용자가
 * 타이핑하는 흐름을 흉내 내고, 시나리오가 소진된 뒤에는 {@code /exit}를 반복 공급해
 * 대화 루프가 입력 대기 상태로 영원히 멈추는 것을 방지한다.</p>
 *
 * <p>입력 에코: 시나리오 라인을 공급하기 직전에 해당 라인을 {@code System.out}에
 * 개행과 함께 에코한다(기본 켬). 콘솔 캡처에 사용자 입력이 함께 남아 대화 흐름을
 * 그대로 읽을 수 있다. {@code -Dchapter3.sim.echo=false}로 끈다.
 * 시나리오 소진 후 안전장치로 반복 공급되는 {@code /exit}는 캡처 오염을 막기 위해
 * 에코하지 않는다.</p>
 */
final class SimulatedCliInput implements AutoCloseable {

    private static final long DEFAULT_LINE_DELAY_MILLIS = 400L;
    private static final String EXIT_COMMAND = "/exit";

    private final PipedInputStream inputStream;
    private final Thread writerThread;
    private volatile boolean closed;

    SimulatedCliInput(List<String> scriptLines) {
        this.inputStream = new PipedInputStream(64 * 1024);
        PipedOutputStream outputStream;
        try {
            outputStream = new PipedOutputStream(this.inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("시뮬레이션 입력 파이프를 만들 수 없습니다.", ex);
        }

        long lineDelayMillis = Long.getLong("chapter3.sim.lineDelayMillis", DEFAULT_LINE_DELAY_MILLIS);
        boolean echoEnabled = Boolean.parseBoolean(System.getProperty("chapter3.sim.echo", "true"));

        this.writerThread = new Thread(() -> {
            try (outputStream) {
                for (String line : scriptLines) {
                    sleep(lineDelayMillis);
                    if (echoEnabled) {
                        // 공급 직전 에코: System.out 이 Tee 로 감싸져 있으면 캡처 파일에도 남는다.
                        System.out.println(line);
                    }
                    writeLine(outputStream, line);
                }
                // 시나리오 소진 후: 대화 루프가 살아 있으면 /exit 를 반복 공급해 블록을 방지한다.
                // (안전장치 입력이므로 에코하지 않는다)
                while (!closed) {
                    sleep(lineDelayMillis);
                    writeLine(outputStream, EXIT_COMMAND);
                }
            } catch (IOException | InterruptedException ignored) {
                // 읽기 쪽(애플리케이션)이 먼저 종료되어 파이프가 닫혔거나(IOException),
                // 테스트가 close()로 스레드를 깨운 경우(InterruptedException) 정상 종료 경로다.
            }
        }, "chapter3-simulated-cli-input");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    InputStream stream() {
        return this.inputStream;
    }

    @Override
    public void close() {
        this.closed = true;
        this.writerThread.interrupt();
        try {
            this.inputStream.close();
        } catch (IOException ignored) {
            // 종료 경로에서의 close 실패는 무시한다.
        }
    }

    private static void writeLine(PipedOutputStream outputStream, String line) throws IOException {
        outputStream.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
