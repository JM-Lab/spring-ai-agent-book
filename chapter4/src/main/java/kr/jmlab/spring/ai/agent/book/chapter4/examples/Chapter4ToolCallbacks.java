package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import kr.jmlab.spring.ai.agent.book.chapter4.support.ToolNames;

/**
 * {@link FunctionToolCallback} 으로 함수형 툴을 구성한다.
 */
public final class Chapter4ToolCallbacks {

    private Chapter4ToolCallbacks() {
    }

    public static ToolCallback discountCalculator() {
        Function<DiscountRequest, String> discountFunction = request -> {
            BigDecimal price = request.price();
            BigDecimal discountRate = request.ratePercent()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discount = price.multiply(discountRate).setScale(0, RoundingMode.HALF_UP);
            BigDecimal finalPrice = price.subtract(discount);

            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            return "정가 %s원에서 %s%% 할인되어 최종 금액은 %s원입니다."
                    .formatted(
                            numberFormat.format(price),
                            request.ratePercent().stripTrailingZeros().toPlainString(),
                            numberFormat.format(finalPrice));
        };

        return FunctionToolCallback
                .builder(ToolNames.DISCOUNT_CALCULATOR, discountFunction)
                .description("정가와 할인율을 받아 할인 후 금액을 계산합니다.")
                .inputType(DiscountRequest.class)
                .build();
    }

    public static ToolCallback customerContactLookup() {
        Function<CustomerContactRequest, CustomerContact> lookupFunction = request -> new CustomerContact(
                request.customerId(),
                "홍길동",
                "hong.gildong@example.com",
                "프리미엄 지원 고객");

        return FunctionToolCallback
                .builder(ToolNames.CUSTOMER_CONTACT_LOOKUP, lookupFunction)
                .description("고객 ID로 고객 연락처와 지원 등급을 조회합니다. 모델에는 이메일이 마스킹된 결과만 전달합니다.")
                .inputType(CustomerContactRequest.class)
                .toolCallResultConverter(new EmailMaskingToolCallResultConverter())
                .build();
    }

    public static ToolCallback sessionSummary(boolean returnDirect) {
        BiFunction<SessionSummaryRequest, ToolContext, String> sessionFunction = (request, context) -> {
            Map<String, Object> values = context.getContext();
            Object userName = values.getOrDefault("userName", "anonymous");
            Object conversationId = values.getOrDefault("conversationId", "unknown");

            return """
                    세션 요약
                    - 사용자: %s
                    - 대화 ID: %s
                    - 학습 주제: %s
                    - 다음 액션: 툴이 필요한 작업과 순수 대화 작업을 구분해 CLI에서 테스트한다.
                    """.formatted(userName, conversationId, request.topic());
        };

        return FunctionToolCallback
                .<SessionSummaryRequest, String>builder(ToolNames.SESSION_SUMMARY, sessionFunction)
                .description("툴 컨텍스트에 담긴 사용자/대화 정보를 사용해 현재 학습 세션을 요약합니다.")
                .inputType(SessionSummaryRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(returnDirect).build())
                .build();
    }

    public record DiscountRequest(BigDecimal price, BigDecimal ratePercent) {
    }

    public record CustomerContactRequest(String customerId) {
    }

    public record CustomerContact(String customerId, String name, String email, String supportTier) {
    }

    public record SessionSummaryRequest(String topic) {
    }
}
