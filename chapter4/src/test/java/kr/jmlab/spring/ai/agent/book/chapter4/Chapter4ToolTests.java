package kr.jmlab.spring.ai.agent.book.chapter4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.CalculatorTools;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.Chapter4ToolCallbacks;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.DateTimeTools;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.ToolDefinitionExamples;
import kr.jmlab.spring.ai.agent.book.chapter4.support.InventoryTools;
import kr.jmlab.spring.ai.agent.book.chapter4.support.TodoTools;
import kr.jmlab.spring.ai.agent.book.chapter4.support.ToolNames;

/**
 * 4장 툴 예제의 콜백 생성과 직접 실행을 검증한다.
 */
class Chapter4ToolTests {

    @Test
    @DisplayName("@Tool 메서드를 ToolCallback 으로 변환한다")
    void methodToolsExposeDefinitions() {
        var callbacks = ToolCallbacks.from(new DateTimeTools(), new CalculatorTools());

        assertThat(Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name()))
                .contains(
                        ToolNames.CURRENT_DATETIME,
                        ToolNames.CURRENT_DATE,
                        ToolNames.CALCULATE,
                        ToolNames.PERCENTAGE
                );
    }

    @Test
    @DisplayName("FunctionToolCallback 할인 툴을 직접 실행한다")
    void functionToolCallbackCanBeCalledDirectly() {
        var discountTool = Chapter4ToolCallbacks.discountCalculator();

        String result = discountTool.call("""
                {"price": 35000, "ratePercent": 18}
                """);

        assertThat(result).contains("28,700원");
    }

    @Test
    @DisplayName("ToolCallResultConverter 로 툴 결과의 이메일을 마스킹한다")
    void toolCallResultConverterMasksEmail() {
        var customerLookupTool = Chapter4ToolCallbacks.customerContactLookup();

        String result = customerLookupTool.call("""
                {"customerId": "C-100"}
                """);

        assertThat(result)
                .contains("C-100")
                .contains("[EMAIL]")
                .doesNotContain("hong.gildong@example.com");
    }

    @Test
    @DisplayName("ToolContext 를 사용하는 세션 요약 툴을 직접 실행한다")
    void contextToolCallbackUsesToolContext() {
        var sessionTool = Chapter4ToolCallbacks.sessionSummary(true);

        String result = sessionTool.call("""
                {"topic": "Tool Calling"}
                """, new ToolContext(Map.of(
                "userName", "tester",
                "conversationId", "test-conversation"
        )));

        assertThat(result)
                .contains("tester")
                .contains("test-conversation")
                .contains("Tool Calling");
    }

    @Test
    @DisplayName("명시적으로 구성한 MethodToolCallback 을 실행한다")
    void explicitMethodToolCallbackCanBeCalledDirectly() {
        var callback = ToolDefinitionExamples.methodToolCallback(new DateTimeTools());

        String result = callback.call("{}");

        assertThat(result).containsPattern("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @DisplayName("최종 프로젝트 지원 툴은 상태를 관리한다")
    void projectSupportToolsManageState() {
        InventoryTools inventoryTools = new InventoryTools();
        TodoTools todoTools = new TodoTools();

        assertThat(inventoryTools.lookupProduct("SKU-100"))
                .contains("Spring AI 입문 워크북")
                .contains("12개");
        assertThat(inventoryTools.reserveProduct("SKU-100", 2))
                .contains("2개를 예약했습니다")
                .contains("10개");

        assertThat(todoTools.addTodo("SKU-100 예약 내역 확인"))
                .contains("할 일 #1");
        assertThat(todoTools.listTodos())
                .contains("SKU-100 예약 내역 확인")
                .contains("진행");
        assertThat(todoTools.completeTodo(1))
                .contains("완료 처리");
    }
}
