package kr.jmlab.spring.ai.agent.book.chapter4;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CLI Step 러너에 사용자 입력을 흉내 내어 공급하는 테스트 헬퍼.
 *
 * <p>파이프({@link PipedInputStream})에 작성 스레드가 질문을 한 줄씩 흘려 넣는다.
 * 줄 사이에는 딜레이(기본 400ms, 시스템 프로퍼티 {@value #DELAY_PROPERTY} 로 조절)를 두어
 * 모델 응답 중에 다음 입력이 몰리지 않게 한다. 준비한 질문을 모두 공급한 뒤에는
 * {@code /exit} 를 계속 반복 공급해, 러너가 어떤 시점에 프롬프트를 다시 열어도
 * 표준 입력 대기 상태로 멈추지 않고 반드시 종료되게 한다.</p>
 *
 * <p>공급하는 줄은 기본적으로 {@link System#out} 에도 에코해({@value #ECHO_PROPERTY}
 * 프로퍼티, 기본 켬) 캡처 파일과 콘솔에서 "질문 → 응답" 흐름을 그대로 읽을 수 있게 한다.
 * 스크립트 소진 후 안전장치로 반복 공급하는 {@code /exit} 는 에코하지 않는다.</p>
 */
final class CliInputSimulator implements AutoCloseable {

    static final String DELAY_PROPERTY = "chapter4.cli.input.delay-ms";

    /** 입력 에코 여부(기본 켬). {@code -Dchapter4.cli.echo=false} 로 끈다. */
    static final String ECHO_PROPERTY = "chapter4.cli.echo";

    private static final long DEFAULT_DELAY_MILLIS = 400L;
    private static final String EXIT_COMMAND = "/exit";
    private static final int PIPE_BUFFER_SIZE = 64 * 1024;

    private final PipedInputStream pipedInputStream;
    private final PipedOutputStream pipedOutputStream;
    private final Thread writerThread;
    private final boolean echoEnabled;

    private CliInputSimulator(List<String> userInputs, long delayBetweenLinesMillis) {
        this.echoEnabled = Boolean.parseBoolean(System.getProperty(ECHO_PROPERTY, "true"));
        this.pipedOutputStream = new PipedOutputStream();
        try {
            this.pipedInputStream = new PipedInputStream(this.pipedOutputStream, PIPE_BUFFER_SIZE);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("입력 시뮬레이션 파이프를 생성하지 못했습니다.", ex);
        }
        this.writerThread = new Thread(
                () -> supplyLines(userInputs, delayBetweenLinesMillis),
                "chapter4-cli-input-simulator");
        this.writerThread.setDaemon(true);
    }

    /**
     * 기본 딜레이(또는 {@value #DELAY_PROPERTY} 프로퍼티 값)로 시뮬레이터를 생성한다.
     */
    static CliInputSimulator withDefaultDelay(String... userInputs) {
        long delayBetweenLinesMillis = Long.getLong(DELAY_PROPERTY, DEFAULT_DELAY_MILLIS);
        return new CliInputSimulator(List.of(userInputs), delayBetweenLinesMillis);
    }

    /**
     * 작성 스레드를 시작하고, {@code System.setIn(...)} 에 넘길 입력 스트림을 반환한다.
     */
    InputStream start() {
        this.writerThread.start();
        return this.pipedInputStream;
    }

    private void supplyLines(List<String> userInputs, long delayBetweenLinesMillis) {
        try {
            for (String userInput : userInputs) {
                writeLine(userInput, true);
                Thread.sleep(delayBetweenLinesMillis);
            }
            writeLine(EXIT_COMMAND, true);
            // 러너가 앞선 질문을 처리하는 동안 프롬프트가 추가로 열릴 수 있으므로
            // 파이프가 닫힐 때까지 /exit 를 반복 공급한다. (안전장치, 에코하지 않음)
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(delayBetweenLinesMillis);
                writeLine(EXIT_COMMAND, false);
            }
        }
        catch (IOException | InterruptedException ex) {
            // 파이프가 닫혔거나(테스트 종료) 인터럽트되면 입력 공급을 멈춘다.
        }
    }

    private void writeLine(String line, boolean echo) throws IOException {
        if (echo && this.echoEnabled) {
            // 공급 직전에 에코해 캡처/콘솔에서 사용자 입력처럼 보이게 한다.
            System.out.println(line);
        }
        this.pipedOutputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        this.pipedOutputStream.flush();
    }

    @Override
    public void close() {
        this.writerThread.interrupt();
        try {
            this.pipedOutputStream.close();
        }
        catch (IOException ignored) {
            // 이미 닫힌 파이프는 무시한다.
        }
        try {
            this.pipedInputStream.close();
        }
        catch (IOException ignored) {
            // 이미 닫힌 파이프는 무시한다.
        }
    }
}
