package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

/**
 * 툴 실행 예외를 사용자에게 전달할 수 있는 메시지로 변환한다.
 */
public class FriendlyToolExceptionProcessor implements ToolExecutionExceptionProcessor {

    @Override
    public String process(ToolExecutionException exception) {
        String toolName = exception.getToolDefinition().name();
        String message = exception.getCause() == null
                ? "원인을 알 수 없습니다."
                : exception.getCause().getMessage();
        return "툴 '%s' 실행 중 문제가 발생했습니다: %s".formatted(toolName, message);
    }
}
