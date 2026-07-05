package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.lang.reflect.Type;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

/**
 * 툴 실행 결과를 모델에 전달하기 전에 이메일을 마스킹한다.
 */
public class EmailMaskingToolCallResultConverter implements ToolCallResultConverter {

    private final ToolCallResultConverter delegate = new DefaultToolCallResultConverter();

    @Override
    public String convert(@Nullable Object result, @Nullable Type returnType) {
        // 기본 변환기로 JSON 문자열을 만든 뒤, 이메일 주소를 마스킹 문자열로 치환
        return delegate.convert(result, returnType)
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[EMAIL]");
    }
}
