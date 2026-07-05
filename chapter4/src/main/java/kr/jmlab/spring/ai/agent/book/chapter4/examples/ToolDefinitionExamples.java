package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.lang.reflect.Method;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import kr.jmlab.spring.ai.agent.book.chapter4.support.ToolNames;

/**
 * ToolDefinition 과 MethodToolCallback 을 명시적으로 구성한다.
 */
public final class ToolDefinitionExamples {

    private ToolDefinitionExamples() {
    }

    public static ToolDefinition manualCurrentDateDefinition() {
        return ToolDefinition.builder()
                .name(ToolNames.CURRENT_DATE)
                .description("오늘 날짜를 yyyy-MM-dd 형식으로 반환합니다.")
                .inputSchema("""
                        {
                          "type": "object",
                          "properties": {},
                          "additionalProperties": false
                        }
                        """)
                .build();
    }

    public static ToolCallback methodToolCallback(DateTimeTools dateTimeTools) {
        try {
            Method method = DateTimeTools.class.getMethod("currentDate");
            return MethodToolCallback.builder()
                    .toolDefinition(manualCurrentDateDefinition())
                    .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                    .toolMethod(method)
                    .toolObject(dateTimeTools)
                    .build();
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("currentDate 메서드를 찾을 수 없습니다.", ex);
        }
    }
}
