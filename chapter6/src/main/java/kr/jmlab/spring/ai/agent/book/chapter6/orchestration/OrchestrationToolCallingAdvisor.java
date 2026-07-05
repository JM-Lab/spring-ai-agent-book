package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.eviction.LruEvictionStrategy;

/**
 * 오케스트레이션 어드바이저: 메타 툴과 도메인 툴을 한 호출 루프에서 두 레이어로 다룬다.
 *
 * <p>{@link ToolSearchToolCallingAdvisor}는 요청의 모든 툴을 색인에 넣고 숨긴 뒤 {@code toolSearchTool} 하나만
 * 노출하고, 검색으로 발견된 툴을 라운드마다 주입한다. 그래서 오케스트레이션 메타 툴(스킬, 하위 에이전트 위임, 작업
 * 계획, 명확화 질문)까지 숨겨져, 모델이 그것들을 의미 검색으로 찾지 못하면 절차가 깨진다.</p>
 *
 * <p>이 어드바이저는 그 위에 한 겹을 더한다. 도메인 툴은 부모처럼 검색 대상으로 두되, <b>메타 툴은 매 라운드
 * 항상 노출</b>한다. 검색 자체(toolSearchTool)도 하나의 메타 툴이므로, 결과적으로 모델은 늘 보이는 메타 툴
 * 묶음(검색 포함) 중에서 고르고, 도메인 능력이 필요하면 검색해서 그때 끌어 쓴다. 부모의 private 내부는 건드리지
 * 않고, 부모가 고른 라운드별 툴셋 위에 메타 툴을 더하는 방식이라 결합이 얕다.</p>
 */
public final class OrchestrationToolCallingAdvisor extends ToolSearchToolCallingAdvisor {

    // 매 라운드 항상 노출할 메타 툴
    private final List<ToolCallback> alwaysOnTools;

    private OrchestrationToolCallingAdvisor(
            ToolCallingManager toolCallingManager,
            int advisorOrder,
            ToolExecutionEligibilityChecker checker,
            ToolIndex toolIndex,
            String systemMessageSuffix,
            Integer maxResults,
            List<ToolCallback> alwaysOnTools) {
        super(toolCallingManager, advisorOrder, checker, toolIndex,
                systemMessageSuffix, true, maxResults, true,
                ChatMemory.CONVERSATION_ID, new LruEvictionStrategy(1000));
        this.alwaysOnTools = List.copyOf(alwaysOnTools);
    }

    /**
     * 메타 툴은 항상 노출, 도메인 툴은 {@code toolIndex}로 검색하는 오케스트레이션 어드바이저를 만든다.
     * 도메인 툴은 에이전트의 일반 툴 목록으로 등록하고(부모가 색인, 숨김), 메타 툴만 여기로 넘긴다.
     *
     * @param toolIndex 도메인 툴 의미 검색용 색인(한 번 만들어 공유)
     * @param alwaysOnTools 매 라운드 항상 노출할 메타 툴
     * @param maxResults 검색이 한 번에 돌려줄 최대 도메인 툴 수
     * @param checker 요청별 안전 가드(툴 루프 상한)
     * @param advisorOrder 어드바이저 순서값
     */
    public static OrchestrationToolCallingAdvisor of(
            ToolIndex toolIndex,
            List<ToolCallback> alwaysOnTools,
            Integer maxResults,
            ToolExecutionEligibilityChecker checker,
            int advisorOrder) {
        return new OrchestrationToolCallingAdvisor(
                ToolCallingManager.builder().build(),
                advisorOrder,
                checker,
                toolIndex,
                buildSystemMessageSuffix(alwaysOnTools),
                maxResults,
                alwaysOnTools
        );
    }

    /**
     * 두 레이어를 모두 설명하는 시스템 메시지 접미사를 만든다. 부모의 기본 접미사는 {@code toolSearchTool}만
     * 안내해, 모델이 항상 노출된 메타 툴까지 검색해야 하는 줄 알고 헛돌 수 있다. 그래서 메타 툴은 검색 없이 바로
     * 호출하고, 도메인 능력만 검색하라고 명시한다.
     */
    private static String buildSystemMessageSuffix(List<ToolCallback> alwaysOnTools) {
        String menu = alwaysOnTools.stream()
                .map(tool -> "- " + tool.getToolDefinition().name() + ": "
                        + firstSentence(tool.getToolDefinition().description()))
                .collect(Collectors.joining("\n"));
        return """

                다음 오케스트레이션 툴은 늘 사용할 수 있습니다. 상황에 맞으면 검색하지 말고 바로 호출하세요:
                %s

                위 목록에 없는 도메인 능력이 필요하면 toolSearchTool로 알맞은 툴을 찾은 뒤 그 툴을 호출하세요.
                검색 결과가 맞지 않으면 다른 표현으로 다시 검색하세요.
                """.formatted(menu);
    }

    /** 메타 툴 설명을 한 줄로 줄인다(첫 문장 또는 첫 줄). */
    private static String firstSentence(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String oneLine = description.strip().split("\\R", 2)[0];
        int period = oneLine.indexOf(". ");
        return period > 0 ? oneLine.substring(0, period + 1) : oneLine;
    }

    @Override
    protected ChatClientRequest doBeforeCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {
        return appendAlwaysOnTools(super.doBeforeCall(request, chain));
    }

    @Override
    protected ChatClientRequest doBeforeStream(
            ChatClientRequest request,
            StreamAdvisorChain chain) {
        return appendAlwaysOnTools(super.doBeforeStream(request, chain));
    }

    /**
     * 부모가 고른 이번 라운드의 툴셋({@code toolSearchTool} ∪ 검색으로 발견된 도메인 툴) 위에 메타 툴을 얹는다.
     * 부모가 설정한 다른 옵션(세션 컨텍스트 등)은 {@code mutate()}로 보존하고 툴 목록만 합친다.
     */
    private ChatClientRequest appendAlwaysOnTools(ChatClientRequest request) {
        if (!(request.prompt().getOptions() instanceof ToolCallingChatOptions options)) {
            return request;
        }
        List<ToolCallback> merged = new ArrayList<>(options.getToolCallbacks());
        Set<String> present = new LinkedHashSet<>();
        merged.forEach(callback -> present.add(callback.getToolDefinition().name()));
        for (ToolCallback metaTool : alwaysOnTools) {
            if (present.add(metaTool.getToolDefinition().name())) {
                merged.add(metaTool);
            }
        }
        ToolCallingChatOptions mergedOptions =
                ((ToolCallingChatOptions.Builder<?>) options.mutate())
                        .toolCallbacks(merged)
                        .build();
        return request.mutate()
                .prompt(request.prompt().mutate().chatOptions(mergedOptions).build())
                .build();
    }
}
