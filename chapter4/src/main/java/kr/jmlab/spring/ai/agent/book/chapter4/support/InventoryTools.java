package kr.jmlab.spring.ai.agent.book.chapter4.support;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 외부 시스템 조회/예약을 흉내 내는 인메모리 상품 재고 툴.
 */
@Component
public class InventoryTools {

    private final Map<String, Product> products = new ConcurrentHashMap<>(Map.of(
            "SKU-100", new Product("SKU-100", "Spring AI 입문 워크북", 12),
            "SKU-200", new Product("SKU-200", "Agent 설계 노트", 7),
            "SKU-300", new Product("SKU-300", "RAG 실습 데이터셋", 4)
    ));

    @Tool(
            name = ToolNames.PRODUCT_LIST,
            description = "예약 가능한 상품 SKU, 이름, 재고 수량을 반환합니다.")
    public String listProducts() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::sku))
                .map(product -> "%s | %s | 재고 %d개".formatted(
                        product.sku(), product.name(), product.stock()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("등록된 상품이 없습니다.");
    }

    @Tool(
            name = ToolNames.PRODUCT_LOOKUP,
            description = "SKU로 상품 이름과 재고 수량을 조회합니다.")
    public String lookupProduct(@ToolParam(description = "조회할 상품 SKU") String sku) {
        Product product = products.get(normalizeSku(sku));
        if (product == null) {
            return "해당 SKU의 상품을 찾을 수 없습니다.";
        }
        return "%s는 '%s'이며 현재 재고는 %d개입니다."
                .formatted(product.sku(), product.name(), product.stock());
    }

    @Tool(
            name = ToolNames.PRODUCT_RESERVE,
            description = "SKU와 수량을 받아 재고를 예약합니다. 재고가 부족하면 실패 사유를 반환합니다.")
    public String reserveProduct(
            @ToolParam(description = "예약할 상품 SKU")
            String sku,
            @ToolParam(description = "예약 수량")
            int quantity) {
        if (quantity <= 0) {
            return "예약 수량은 1개 이상이어야 합니다.";
        }

        String normalizedSku = normalizeSku(sku);
        Product product = products.get(normalizedSku);
        if (product == null) {
            return "해당 SKU의 상품을 찾을 수 없습니다.";
        }
        if (product.stock() < quantity) {
            return "%s의 재고가 부족합니다. 현재 재고는 %d개입니다."
                    .formatted(product.sku(), product.stock());
        }

        products.put(normalizedSku, new Product(product.sku(), product.name(), product.stock() - quantity));
        return "%s %d개를 예약했습니다. 남은 재고는 %d개입니다."
                .formatted(product.name(), quantity, product.stock() - quantity);
    }

    private String normalizeSku(String sku) {
        return sku == null ? "" : sku.trim().toUpperCase();
    }

    private record Product(String sku, String name, int stock) {
    }
}
