package kr.jmlab.spring.ai.agent.book.chapter6.capability;

/**
 * 6장 예제에서 사용하는 Tool 이름을 한 곳에서 관리한다.
 */
public final class ToolNames {

    public static final String CURRENT_DATETIME = "current_datetime";
    public static final String CURRENT_DATE = "current_date";
    public static final String CALCULATE = "calculate";
    public static final String PERCENTAGE = "percentage";
    public static final String DISCOUNT_CALCULATOR = "discount_calculator";
    public static final String CUSTOMER_CONTACT_LOOKUP = "customer_contact_lookup";
    public static final String SESSION_SUMMARY = "session_summary";
    public static final String TODO_ADD = "todo_add";
    public static final String TODO_LIST = "todo_list";
    public static final String TODO_COMPLETE = "todo_complete";
    public static final String PRODUCT_LIST = "product_list";
    public static final String PRODUCT_LOOKUP = "product_lookup";
    public static final String PRODUCT_RESERVE = "product_reserve";

    private ToolNames() {
    }
}
