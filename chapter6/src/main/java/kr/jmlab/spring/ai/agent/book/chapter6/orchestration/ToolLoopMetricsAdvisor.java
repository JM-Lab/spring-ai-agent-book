package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 툴 호출 루프 관측: 커스텀 관측 지표를 루프에 붙이는 방법.
 *
 * <p>{@link BaseAdvisor}라서 호출(call), 스트리밍(stream) 양쪽에서 동작하며, 순서값을
 * ToolCallingAdvisor(기본 {@code HIGHEST_PRECEDENCE + 300})보다 큰 {@code +400}으로 두어
 * 툴 호출 루프 <b>안쪽</b>에 놓이므로 루프가 도는 매 반복마다 {@link #after}가 호출된다.
 * 반복별 <b>로깅</b>은 내장 {@code SimpleLoggerAdvisor}(기본 order 0, 역시 루프 안)가 담당하므로,
 * 이 어드바이저는 루프-레벨 <b>관측 지표</b>만 발행한다(반복 수, 툴별 호출 수, 반복당 토큰).
 * 발행한 지표는 앞에서 구성한 OTLP 익스포터를 타고 외부 관측 시스템으로 전송된다.</p>
 */
public class ToolLoopMetricsAdvisor implements BaseAdvisor {

    private final MeterRegistry registry;

    public ToolLoopMetricsAdvisor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request; // 요청은 변형하지 않음
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return response;
        }
        // 루프 1회 반복마다 한 번 집계
        registry.counter("agent.tool.loop.iterations").increment();
        chatResponse.getResult().getOutput().getToolCalls()
                .forEach(toolCall ->
                        registry.counter("agent.tool.calls", "tool", toolCall.name()).increment());
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage != null && usage.getTotalTokens() != null) {
            DistributionSummary.builder("agent.tool.loop.tokens")
                    .baseUnit("tokens")
                    .register(registry)
                    .record(usage.getTotalTokens());
        }
        return response;
    }

    @Override
    public int getOrder() {
        // ToolCallingAdvisor(+300) 안쪽이므로 매 반복 실행
        return BaseAdvisor.HIGHEST_PRECEDENCE + 400;
    }
}
