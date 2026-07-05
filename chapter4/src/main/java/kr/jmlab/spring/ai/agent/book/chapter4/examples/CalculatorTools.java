package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter4.support.ToolNames;

/**
 * {@link ToolParam @ToolParam} 으로 파라미터 의미를 명확히 표현하는 계산 툴.
 */
@Component
public class CalculatorTools {

    @Tool(
            name = ToolNames.CALCULATE,
            description = "두 숫자에 대해 add, subtract, multiply, divide 중 하나의 연산을 수행합니다.")
    public String calculate(
            @ToolParam(description = "연산 이름. add, subtract, multiply, divide 중 하나")
            String operation,
            @ToolParam(description = "첫 번째 숫자")
            BigDecimal left,
            @ToolParam(description = "두 번째 숫자")
            BigDecimal right) {
        return switch (operation.toLowerCase()) {
            case "add" -> format(left.add(right));
            case "subtract" -> format(left.subtract(right));
            case "multiply" -> format(left.multiply(right));
            case "divide" -> {
                if (BigDecimal.ZERO.compareTo(right) == 0) {
                    yield "0으로 나눌 수 없습니다.";
                }
                yield format(left.divide(right, 4, RoundingMode.HALF_UP));
            }
            default -> "지원하지 않는 연산입니다. add, subtract, multiply, divide 중 하나를 사용하세요.";
        };
    }

    @Tool(
            name = ToolNames.PERCENTAGE,
            description = "전체 값에서 일부 값이 차지하는 비율을 퍼센트로 계산합니다.")
    public String percentage(
            @ToolParam(description = "일부 값")
            BigDecimal part,
            @ToolParam(description = "전체 값")
            BigDecimal total) {
        if (BigDecimal.ZERO.compareTo(total) == 0) {
            return "전체 값이 0이면 비율을 계산할 수 없습니다.";
        }
        BigDecimal percent = part
                .divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return format(percent) + "%";
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
