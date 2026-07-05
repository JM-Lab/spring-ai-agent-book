package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * SearchRequest 와 메타데이터 필터 작성 예제.
 */
public final class SearchRequestExamples {

    private SearchRequestExamples() {
    }

    public static SearchRequest basic(String query) {
        return SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.5)
                .build();
    }

    public static SearchRequest withTextFilter(String query, String category) {
        return SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThresholdAll()
                .filterExpression("category == '" + category + "' && isActive == true")
                .build();
    }

    public static SearchRequest withTypeSafeFilter(String query) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.and(
                builder.in("category", "tech_docs", "product_catalog"),
                builder.eq("isActive", true)
        ).build();

        return SearchRequest.builder()
                .query(query)
                .topK(8)
                .similarityThreshold(0.50)
                .filterExpression(filter)
                .build();
    }

    public static Map<String, SearchRequest> compare(String query) {
        Map<String, SearchRequest> requests = new LinkedHashMap<>();
        requests.put("basic", basic(query));
        requests.put("textFilter", withTextFilter(query, "tech_docs"));
        requests.put("typeSafeFilter", withTypeSafeFilter(query));
        return requests;
    }
}
