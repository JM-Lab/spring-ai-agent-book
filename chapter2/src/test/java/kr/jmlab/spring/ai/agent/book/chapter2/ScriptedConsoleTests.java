package kr.jmlab.spring.ai.agent.book.chapter2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ScriptedConsole} 단위 테스트 (Ollama 불필요, 일반 {@code ./mvnw test} 대상).
 */
class ScriptedConsoleTests {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("스크립트 줄을 순서대로 공급하고, 소진 후에는 /exit 를 반복 공급한다")
    void suppliesScriptThenExitForever() throws Exception {
        try (ScriptedConsole console = ScriptedConsole.withDefaultDelay("첫 번째 질문", "두 번째 질문");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(console.start(), StandardCharsets.UTF_8))) {

            assertEquals("첫 번째 질문", reader.readLine());
            assertEquals("두 번째 질문", reader.readLine());
            // 스크립트 소진 후 안전장치: /exit 가 계속 공급되어 절대 블록되지 않는다.
            assertEquals("/exit", reader.readLine());
            assertEquals("/exit", reader.readLine());
        }
    }
}
