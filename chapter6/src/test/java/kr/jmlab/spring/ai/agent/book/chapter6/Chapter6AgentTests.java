package kr.jmlab.spring.ai.agent.book.chapter6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.AgentSafety;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.ToolLoopMetricsAdvisor;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.local.ChainWorkflow;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.local.InventoryTools;

/**
 * 6장 에이전트 핵심 로직 단위 검증: Ollama 없이 동작한다.
 *
 * <ul>
 * <li>안전 가드: 툴 호출 루프가 상한을 넘으면 강제 종료하고, 요청마다 새 체커가 0부터 독립적으로 세는지.</li>
 *   <li>로컬 재고 툴: 조회, 예약 상태 변화.</li>
 *   <li>프롬프트 체이닝 워크플로: 단계별 시스템 프롬프트 적용과 출력→입력 전달.</li>
 * </ul>
 */
class Chapter6AgentTests {

    @Test
    @DisplayName("안전 가드는 툴 루프를 상한에서 멈추고, 요청마다 새 체커가 0부터 독립적으로 센다")
    void safetyGuardCapsToolRoundsPerRequest() {
        ChatResponse withToolCalls = mock(ChatResponse.class);
        when(withToolCalls.hasToolCalls()).thenReturn(true);
        ChatResponse withoutToolCalls = mock(ChatResponse.class);
        when(withoutToolCalls.hasToolCalls()).thenReturn(false);

        // 한 요청용 체커: 상한(3)까지는 루프 계속, 4번째 라운드에서 강제 종료
        ToolExecutionEligibilityChecker checker = AgentSafety.maxToolRounds(3);
        assertThat(checker.apply(withToolCalls)).isTrue();    // 라운드 1
        assertThat(checker.apply(withToolCalls)).isTrue();    // 라운드 2
        assertThat(checker.apply(withToolCalls)).isTrue();    // 라운드 3
        assertThat(checker.apply(withToolCalls)).isFalse();   // 라운드 4 → 상한 초과, 중단

        // 모델이 툴을 더 부르지 않으면 정상 종료(false)
        assertThat(AgentSafety.maxToolRounds(3).apply(withoutToolCalls)).isFalse();

        // 요청마다 새 체커를 만들면 카운터를 공유하지 않아 각각 0부터 센다(동시 요청에도 안전)
        ToolExecutionEligibilityChecker freshChecker = AgentSafety.maxToolRounds(3);
        assertThat(freshChecker.apply(withToolCalls)).isTrue();   // 새 요청의 라운드 1

        // null 응답은 안전하게 false
        assertThat(AgentSafety.maxToolRounds(3).apply(null)).isFalse();
    }

    @Test
    @DisplayName("로컬 재고 툴은 조회, 예약 상태를 관리한다")
    void inventoryToolsManageState() {
        InventoryTools inventoryTools = new InventoryTools();

        assertThat(inventoryTools.lookupProduct("SKU-200"))
                .contains("Agent 설계 노트")
                .contains("7개");
        assertThat(inventoryTools.reserveProduct("SKU-200", 2))
                .contains("2개를 예약했습니다")
                .contains("5개");
        assertThat(inventoryTools.reserveProduct("SKU-200", 99))
                .contains("재고가 부족");
    }

    @Test
    @DisplayName("프롬프트 체이닝은 각 시스템 프롬프트를 순차 적용하고 이전 출력을 다음 입력으로 넘긴다")
    void chainWorkflowThreadsOutputs() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("STEP1-OUT", "STEP2-OUT");

        ChainWorkflow workflow = new ChainWorkflow(chatClient, new String[] {"분석가 프롬프트", "검수자 프롬프트"});
        String result = workflow.chain("원본 요청");

        assertThat(result).isEqualTo("STEP2-OUT");   // 마지막 단계 출력이 최종 결과

        ArgumentCaptor<String> inputs = ArgumentCaptor.forClass(String.class);
        verify(chatClient, times(2)).prompt(inputs.capture());
        List<String> allInputs = inputs.getAllValues();
        assertThat(allInputs.get(0)).contains("분석가 프롬프트").contains("원본 요청");
        assertThat(allInputs.get(1)).contains("검수자 프롬프트").contains("STEP1-OUT");   // 출력→입력 전달
    }

    @Test
 @DisplayName("툴 루프 관측 어드바이저는 반복, 툴별 호출, 반복당 토큰 지표를 발행한다")
    void toolLoopMetricsAdvisorPublishesCustomMetrics() {
        // 한 번의 루프 반복에서 같은 툴(check_stock)을 두 번 호출하고 토큰 42를 쓴 응답을 흉내 낸다.
        AssistantMessage output = mock(AssistantMessage.class);
        when(output.getToolCalls()).thenReturn(List.of(
                new AssistantMessage.ToolCall("id1", "function", "check_stock", "{}"),
                new AssistantMessage.ToolCall("id2", "function", "check_stock", "{}")));
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(output);
        Usage usage = mock(Usage.class);
        when(usage.getTotalTokens()).thenReturn(42);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getUsage()).thenReturn(usage);
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatResponse.getMetadata()).thenReturn(metadata);
        ChatClientResponse advised = mock(ChatClientResponse.class);
        when(advised.chatResponse()).thenReturn(chatResponse);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ToolLoopMetricsAdvisor(registry).after(advised, mock(AdvisorChain.class));

        // 자동 계측이 아니라 우리가 더한 도메인 지표가 MeterRegistry에 발행된다.
        assertThat(registry.get("agent.tool.loop.iterations").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("agent.tool.calls").tag("tool", "check_stock").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("agent.tool.loop.tokens").summary().totalAmount()).isEqualTo(42.0);
    }
}
