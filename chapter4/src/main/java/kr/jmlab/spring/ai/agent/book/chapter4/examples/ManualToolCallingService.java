package kr.jmlab.spring.ai.agent.book.chapter4.examples;

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.stereotype.Service;

import kr.jmlab.spring.ai.agent.book.chapter4.support.InventoryTools;

/**
 * 모델 응답의 툴 호출을 애플리케이션 코드가 직접 실행한다.
 */
@Service
public class ManualToolCallingService {

    private static final int MAX_TOOL_LOOPS = 3;
    private static final SystemMessage SYSTEM_MESSAGE = new SystemMessage("""
            당신은 툴 실행 흐름을 설명하는 AI 어시스턴트입니다.
            계산, 날짜, 상품 조회가 필요하면 제공된 툴을 호출하고,
            툴 결과를 받은 뒤 사용자에게 간결하게 답변합니다.
            """);

    private final ChatModel chatModel;
    private final ToolCallback[] toolCallbacks;
    private final ToolCallingManager toolCallingManager;

    public ManualToolCallingService(
            ChatModel chatModel,
            DateTimeTools dateTimeTools,
            CalculatorTools calculatorTools,
            InventoryTools inventoryTools) {
        this.chatModel = chatModel;
        this.toolCallbacks = ToolCallbacks.from(dateTimeTools, calculatorTools, inventoryTools);
        this.toolCallingManager = ToolCallingManager.builder()
                .toolCallbackResolver(new StaticToolCallbackResolver(List.of(this.toolCallbacks)))
                .toolExecutionExceptionProcessor(new FriendlyToolExceptionProcessor())
                .build();
    }

    public String ask(String question) {
        // ChatModel 직접 호출은 툴을 자동 실행하지 않으므로, 옵션에는 툴 목록만 담음
        var options = OllamaChatOptions.builder()
                .toolCallbacks(this.toolCallbacks)
                .temperature(0.1)
                .build();

        Prompt prompt = new Prompt(List.of(SYSTEM_MESSAGE, new UserMessage(question)), options);
        ChatResponse response = chatModel.call(prompt);

        for (int loop = 0; loop < MAX_TOOL_LOOPS && response.hasToolCalls(); loop++) {
            // 수동 실행
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response);
            // 직접 반환 처리
            if (toolExecutionResult.returnDirect()) {
                return ToolExecutionResult.buildGenerations(toolExecutionResult).getFirst().getOutput().getText();
            }

            // 대화 이력을 더해 모델 재호출
            List<Message> conversationHistory = toolExecutionResult.conversationHistory();
            prompt = new Prompt(conversationHistory, options);
            response = chatModel.call(prompt);
        }

        return response.getResult().getOutput().getText();
    }
}
