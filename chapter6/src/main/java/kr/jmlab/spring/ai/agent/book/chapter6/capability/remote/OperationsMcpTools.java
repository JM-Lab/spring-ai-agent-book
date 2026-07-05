package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Step 4: 운영(주문, 재고) MCP 툴. 위험 작업의 server-side 승인(휴먼-인-더-루프 ②)을 보여 준다.
 *
 * <p>{@code place_purchase_order}는 비가역 작업이므로, 실행 직전 {@link McpSyncServerExchange#createElicitation}
 * 으로 승인을 요청한다. 이 요청은 MCP 경계를 거슬러 올라가 클라이언트의 {@code @McpElicitation} 핸들러
 * (ConsoleElicitationHandler)가 사람에게 묻고, 거절되면 발주는 실행되지 않는다.</p>
 */
@Component
@Profile("ops")
public class OperationsMcpTools {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "SKU-100", 12,
            "SKU-200", 7,
            "SKU-300", 4));

    @McpTool(name = "check_stock", description = "SKU의 현재 재고 수량을 조회한다.")
    public String checkStock(
            @McpToolParam(description = "조회할 상품 SKU", required = true) String sku) {
        Integer quantity = stock.get(normalize(sku));
        return quantity == null
                ? "해당 SKU의 상품을 찾을 수 없습니다."
                : "%s의 현재 재고는 %d개입니다.".formatted(normalize(sku), quantity);
    }

    // place_purchase_order 는 Step 3 에서 확인
    @McpTool(name = "place_purchase_order",
            description = "지정한 SKU를 지정 수량만큼 실제로 발주한다. 발주가 필요하면 이 툴을 직접 호출하라.")
    public String placePurchaseOrder(
            @McpToolParam(description = "발주할 상품 SKU", required = true) String sku,
            @McpToolParam(description = "발주 수량", required = true) int quantity,
            McpSyncServerExchange exchange) {
        McpSchema.ElicitResult approval = exchange.createElicitation(
                McpSchema.ElicitFormRequest.builder(
                        "place_purchase_order 툴로 %s %d개를 실제로 발주합니다. 진행할까요?"
                                .formatted(normalize(sku), quantity),
                        Map.of("type", "object", "properties", Map.of()))  // 승인만 받는 빈 폼
                        .build());

        if (approval.action() != McpSchema.ElicitResult.Action.ACCEPT) {
            return "사용자가 승인하지 않아 발주를 취소했습니다.";
        }
        stock.merge(normalize(sku), quantity, Integer::sum);
        return "%s %d개를 발주했습니다. (승인 완료)".formatted(normalize(sku), quantity);
    }

    private String normalize(String sku) {
        return sku == null ? "" : sku.trim().toUpperCase();
    }
}
