package kr.jmlab.spring.ai.agent.book.chapter3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 3장 오프라인 Step 러너 단위 테스트 (Ollama 불필요).
 *
 * <p>Step 1~3은 문서 읽기, 정제, 파일 적재만 수행하므로 외부 모델 없이 실행된다.
 * 실제 Ollama(qwen3.5:4b, bge-m3)와 통신하는 Step 4 이후는
 * {@link Chapter3StepRunnerIT} 통합 테스트(./mvnw verify)에서 실행한다.</p>
 */
class Chapter3StepRunnerTests {

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
    @DisplayName("ch3-step1, DocumentReader")
    void step1_documentReaders() {
        runWithStep("ch3-step1");
    }

    @Test
    @DisplayName("ch3-step2, DocumentTransformer")
    void step2_documentTransformers() {
        runWithStep("ch3-step2");
    }

    @Test
    @DisplayName("ch3-step3, DocumentWriter")
    void step3_documentWriters() {
        runWithStep("ch3-step3");
    }

    private void runWithStep(String step) {
        System.setIn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        String[] args = {"--spring.ai.cli.step=" + step};
        try (ConfigurableApplicationContext ctx =
                     SpringApplication.run(Chapter3Application.class, args)) {
            // CommandLineRunner 는 run() 반환 전에 이미 실행되었다.
        }
    }
}
