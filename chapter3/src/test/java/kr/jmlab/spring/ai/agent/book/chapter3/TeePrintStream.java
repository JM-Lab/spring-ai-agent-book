package kr.jmlab.spring.ai.agent.book.chapter3;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 콘솔과 캡처 파일 두 곳에 동시에 출력하는 {@link PrintStream}.
 *
 * <p>인코딩, 포맷팅({@code println}, autoFlush 등)은 {@link PrintStream}이 담당하고,
 * 그 결과 바이트를 두 스트림에 복제하는 일은 아래 private {@link Fanout} 이 담당한다.
 * 인코딩이 한 번만 수행되어 두 스트림이 완전히 동일한 바이트를 받고, {@code autoFlush=true} 라
 * 토큰이 도착하는 대로 즉시 흘러나간다.</p>
 */
final class TeePrintStream extends PrintStream {

    TeePrintStream(OutputStream console, OutputStream captureFile) {
        super(new Fanout(console, captureFile), true, StandardCharsets.UTF_8);
    }

    /** 한 번의 바이트 쓰기를 콘솔과 캡처 파일 양쪽에 그대로 전달한다. */
    private static final class Fanout extends OutputStream {

        private final OutputStream console;

        private final OutputStream captureFile;

        Fanout(OutputStream console, OutputStream captureFile) {
            this.console = console;
            this.captureFile = captureFile;
        }

        @Override
        public void write(int singleByte) throws IOException {
            this.console.write(singleByte);
            this.captureFile.write(singleByte);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            this.console.write(buffer, offset, length);
            this.captureFile.write(buffer, offset, length);
        }

        @Override
        public void flush() throws IOException {
            this.console.flush();
            this.captureFile.flush();
        }

        @Override
        public void close() throws IOException {
            // 콘솔은 JVM 소유이므로 닫지 않고, 캡처 파일만 닫는다.
            flush();
            this.captureFile.close();
        }
    }
}
