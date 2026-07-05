package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

/**
 * 툴 콜백을 모아 이름 기반으로 해석할 수 있는 카탈로그.
 */
public class ToolCatalog {

    private final List<ToolCallback> callbacks;

    public ToolCatalog(Object... toolObjects) {
        this.callbacks = Arrays.asList(ToolCallbacks.from(toolObjects));
    }

    public List<ToolDefinition> definitions() {
        return callbacks.stream()
                .map(ToolCallback::getToolDefinition)
                .toList();
    }

    public ToolCallbackResolver resolver() {
        return new StaticToolCallbackResolver(callbacks);
    }

    public ToolCallback resolve(String toolName) {
        return resolver().resolve(toolName);
    }
}
