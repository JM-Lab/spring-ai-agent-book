package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import java.util.List;
import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;

import reactor.core.publisher.Flux;

// 코어 에이전트: ChatClient를 직접 만들되, 달라지는 값(프롬프트, 툴, 루프 어드바이저 팩토리)은 생성자로 받음
public class SpringAIAgent {

    private final ChatClient chatClient;
    // 툴 루프 어드바이저는 요청마다 새로 생성(안전 가드의 카운터를 요청 사이에 공유하지 않으려고)
    private final Supplier<ToolCallingAdvisor> loopAdvisorFactory;

    public SpringAIAgent(ChatClient.Builder builder,
                         String systemPrompt,                              // 시스템 프롬프트(코어/강화가 다름)
                         List<ToolCallback> tools,                         // 등록할 툴(로컬, 커뮤니티, MCP, 모두 ToolCallback으로 통일)
                         Supplier<ToolCallingAdvisor> loopAdvisorFactory,  // 툴 루프 어드바이저 팩토리(요청마다 새로)
                         ChatMemory chatMemory,
                         MeterRegistry meterRegistry) {
        this.loopAdvisorFactory = loopAdvisorFactory;
        this.chatClient = builder.clone()
                .defaultSystem(systemPrompt)
                .defaultTools(tools.toArray())
                .defaultAdvisors(                                  // 루프 어드바이저는 여기 말고 run()에서 요청마다 끼움
                        MessageChatMemoryAdvisor.builder(chatMemory)   // 루프 바깥, 1회
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200).build(),
                        new ToolCallTraceAdvisor(),                    // 호출된 툴과 결과를 CLI에 표시(+350, 루프 안)
                        new ThinkTraceAdvisor(),                       // 모델 사고를 CLI에 표시(+360, 루프 안)
                        new SimpleLoggerAdvisor(),                     // 반복별 원문 로깅(0)
                        new ToolLoopMetricsAdvisor(meterRegistry))     // 루프-레벨 지표(+400, 관측 6.6.7)
                .build();
    }

    public Flux<String> run(String userMessage, String conversationId) {
        return chatClient.prompt()
                .advisors(loopAdvisorFactory.get())   // 요청마다 새 툴 루프 어드바이저(+300, 안전 가드도 새 카운터)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(userMessage)
                .stream().content();
    }
}
