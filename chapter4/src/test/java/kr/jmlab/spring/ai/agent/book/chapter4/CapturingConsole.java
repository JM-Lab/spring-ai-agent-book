package kr.jmlab.spring.ai.agent.book.chapter4;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IT 실행 동안 {@code System.out} 을 콘솔과 캡처 파일({@code target/ch4-capture/<step>.txt})로
 * 동시에 흘려보내는 {@link AutoCloseable}.
 *
 * <p>생성 시 현재 {@code System.out} 을 보관하고 {@link TeePrintStream} 으로 교체한다.
 * 파일에는 토큰 단위로 즉시 기록되어 다른 터미널의 {@code tail -f} 로 실시간 스트리밍을 볼 수 있다.
 * {@code close()} 에서 원래 {@code System.out} 을 복원하고 캡처 파일을 닫는다.</p>
 *
 * <pre>{@code
 * try (ScriptedConsole in = ...; CapturingConsole out = CapturingConsole.forStep(step)) {
 *     System.setIn(in.inputStream());
 *     SpringApplication.run(App.class, args).close();
 * }
 * }</pre>
 */
final class CapturingConsole implements AutoCloseable {

    private static final Path CAPTURE_DIR = Path.of("target", "ch4-capture");

    private final PrintStream originalOut;

    private final PrintStream teeOut;

    private CapturingConsole(String step) {
        this.originalOut = System.out;
        OutputStream captureFile = openCaptureFile(step);
        this.teeOut = new TeePrintStream(this.originalOut, captureFile);
        System.setOut(this.teeOut);
    }

    /** 지정한 스텝 이름으로 캡처를 시작한다({@code target/ch4-capture/<step>.txt}). */
    static CapturingConsole forStep(String step) {
        return new CapturingConsole(step);
    }

    /** 캡처 파일의 경로(닫은 뒤 내용을 검증용으로 다시 읽을 때 사용). */
    static Path captureFileOf(String step) {
        return CAPTURE_DIR.resolve(step + ".txt");
    }

    private static OutputStream openCaptureFile(String step) {
        try {
            Files.createDirectories(CAPTURE_DIR);
            return Files.newOutputStream(captureFileOf(step));
        }
        catch (IOException e) {
            throw new UncheckedIOException("캡처 파일 생성 실패: " + step, e);
        }
    }

    @Override
    public void close() {
        System.setOut(this.originalOut);
        // TeePrintStream.close() 는 캡처 파일만 닫고 콘솔(원래 out)은 살려 둔다.
        this.teeOut.close();
    }
}
