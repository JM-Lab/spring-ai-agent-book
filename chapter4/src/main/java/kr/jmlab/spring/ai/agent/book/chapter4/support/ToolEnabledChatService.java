package kr.jmlab.spring.ai.agent.book.chapter4.support;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

import kr.jmlab.spring.ai.agent.book.chapter4.examples.CalculatorTools;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.Chapter4ToolCallbacks;
import kr.jmlab.spring.ai.agent.book.chapter4.examples.DateTimeTools;
import reactor.core.publisher.Flux;

/**
 * 4장 최종 프로젝트: 메모리와 툴 호출을 함께 사용하는 CLI 챗봇 서비스.
 */
@Service
public class ToolEnabledChatService {

    private static final String SYSTEM_PROMPT = """
            당신은 Spring AI Tool Calling 학습을 돕는 친절한 AI 어시스턴트입니다.
            다음 원칙을 지킵니다:
            - 날짜, 계산, 할인, 고객 연락처, 할 일, 상품 재고 같은 작업은 제공된 툴을 사용합니다.
            - 툴 결과를 사용자에게 자연스러운 문장으로 요약합니다.
            - 툴로 확인하지 않은 사실은 단정하지 않습니다.
            - 답변은 한국어로 간결하게 작성합니다.
            """;

    private final ChatClient chatClient;

    public ToolEnabledChatService(
            ChatClient.Builder builder,
            DateTimeTools dateTimeTools,
            CalculatorTools calculatorTools,
            TodoTools todoTools,
            InventoryTools inventoryTools) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = builder.clone()
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(dateTimeTools, calculatorTools, todoTools, inventoryTools)
                .defaultTools(
                        Chapter4ToolCallbacks.discountCalculator(),
                        Chapter4ToolCallbacks.customerContactLookup(),
                        Chapter4ToolCallbacks.sessionSummary(false)
                )
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                                .build(),
                        ToolCallingAdvisor.builder()
                                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                                .build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    public Flux<String> stream(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .toolContext(Map.of(
                        "userName", "chapter4-cli-user",
                        "conversationId", conversationId
                ))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
