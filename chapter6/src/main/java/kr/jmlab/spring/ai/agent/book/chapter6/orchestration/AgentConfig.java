package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;

import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentReferences;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentType;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import kr.jmlab.spring.ai.agent.book.chapter6.capability.local.CalculatorTools;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.local.DateTimeTools;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.local.InventoryTools;

/**
 * 메인 에이전트(클라이언트) 빈 구성.
 *
 * <p>메인 에이전트 클래스({@link SpringAIAgent})는 ChatClient를 직접 만들되 코어, 강화에서 달라지는 값
 * (시스템 프롬프트, 툴, 툴 호출 루프 어드바이저)을 생성자로 받는다. 그 값을 정해 에이전트를 만드는 곳이
 * 여기다. {@code coreAgent}는 로컬 툴로, {@code enhancedAgent}는 거기에 커뮤니티 능력(계획, 질문, 위임, 스킬)과
 * MCP 서버를 더해 만든다. 같은 {@link SpringAIAgent}에 다른 값을 넣는 것이 곧 코어와 강화의 차이다(OCP).</p>
 *
 * <p>코어는 도메인 툴이 많아지면 동적 툴 발견(Tool Search)으로 넘어간다({@link #shouldUseToolSearch}). 강화가
 * 더하는 커뮤니티 능력(SkillsTool, TaskTool, TodoWrite, AskUser)은 오케스트레이션 메타 툴이라 늘 노출돼야 한다.
 * 그래서 강화는 {@link OrchestrationToolCallingAdvisor}로, 메타 툴(검색 툴 포함)은 같은 레이어로 항상 노출하고
 * 도메인 툴만 의미 검색으로 끌어 쓴다. 메타 툴까지 검색 뒤로 숨기면 모델이 재발주 절차 같은 메타 툴을 찾지
 * 못해 절차가 깨지기 때문이다.</p>
 */
@Configuration
@Profile("!ops & !knowledge")
public class AgentConfig {

    /** 한 요청에서 허용할 최대 툴 실행 라운드 수(안전 가드). */
    private static final int MAX_TOOL_ROUNDS = 10;
    /** 툴 루프 어드바이저 순서값(메모리 +200보다 안쪽). */
    private static final int LOOP_ADVISOR_ORDER = BaseAdvisor.HIGHEST_PRECEDENCE + 300;
    /** 동적 툴 발견 시 한 번에 활성화할 최대 툴 수(의미 검색 상위 N개). */
    private static final int TOOL_SEARCH_MAX_RESULTS = 5;

    /** 코어 메인 에이전트. 로컬 도메인 툴 + 동적 툴 발견(선택) + 안전 가드 + 코어 페르소나. */
    @Bean
    SpringAIAgent coreAgent(ChatClient.Builder builder,
                            List<ToolCallback> localTools,
                            ObjectProvider<SyncMcpToolCallbackProvider> mcpToolsProvider,
                            ChatMemory chatMemory,
                            MeterRegistry meterRegistry,
                            ObjectProvider<VectorStore> vectorStoreProvider,
                            @Value("${spring.ai.cli.agent.core.system-prompt}") String systemPrompt,
                            @Value("${spring.ai.cli.tool-search.min-tools:10}") int toolSearchThreshold) {
        SyncMcpToolCallbackProvider mcpTools = mcpToolsProvider.getIfAvailable();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        int remoteCount = mcpTools != null ? mcpTools.getToolCallbacks().length : 0;
        int toolCount = localTools.size() + remoteCount;   // 동적 발견 판단 기준: 도메인 툴 수(로컬 + 원격)

        boolean useToolSearch = shouldUseToolSearch(toolCount, toolSearchThreshold, vectorStore);
        // 어떤 어드바이저로 갈렸는지 시작 시 한 줄로 남김(동적 툴 발견 자동 전환 확인용)
        System.out.printf("[SpringAIAgent] 등록 툴 %d개(로컬 %d + 원격 %d), 임계값 %d → %s%n",
                toolCount, localTools.size(), remoteCount, toolSearchThreshold,
                useToolSearch ? "동적 툴 발견(ToolSearchToolCallingAdvisor, VectorToolIndex)"
                              : "전체 툴 직접 로드(ToolCallingAdvisor)");
        Supplier<ToolCallingAdvisor> loopAdvisorFactory = loopAdvisorFactory(useToolSearch, vectorStore);

        // 등록할 툴: 로컬 도메인 툴 +(연결됐다면) MCP 서버 툴(Provider를 ToolCallback으로 펼쳐 통일)
        List<ToolCallback> tools = new ArrayList<>(localTools);
        if (mcpTools != null) {
            tools.addAll(List.of(mcpTools.getToolCallbacks()));
        }
        return new SpringAIAgent(builder, systemPrompt, tools, loopAdvisorFactory, chatMemory, meterRegistry);
    }

    @Bean   // 강화 에이전트: 도메인 툴 묶음(localTools)과 메타 툴 묶음(metaTools)을 받기만 함
    SpringAIAgent enhancedAgent(ChatClient.Builder builder,
                                List<ToolCallback> localTools,
                                @Qualifier("metaTools") List<ToolCallback> metaTools,   // @Primary(localTools)를 누르고 메타 묶음을 정확히 주입
                                ObjectProvider<SyncMcpToolCallbackProvider> mcpToolsProvider,
                                ObjectProvider<VectorStore> vectorStoreProvider,    // 도메인 툴 의미 검색용 색인
                                ChatMemory chatMemory, MeterRegistry meterRegistry,
                                @Value("${spring.ai.cli.agent.enhanced.system-prompt}") String systemPrompt) {  // 강화 페르소나
        // 도메인 툴(로컬 @Tool + MCP 서버 툴): 검색 대상. 에이전트의 일반 툴 목록으로 등록하면 어드바이저가 색인
        List<ToolCallback> domainTools = new ArrayList<>(localTools);
        SyncMcpToolCallbackProvider mcpTools = mcpToolsProvider.getIfAvailable();
        if (mcpTools != null) domainTools.addAll(List.of(mcpTools.getToolCallbacks()));

        // 오케스트레이션 어드바이저: 메타 툴(검색 툴 포함)은 늘 노출, 도메인 툴만 검색(색인은 한 번 만들어 공유)
        ToolIndex toolIndex = new VectorToolIndex(vectorStoreProvider.getObject());
        System.out.printf("[SpringAIAgent] 강화: 메타 툴 %d개 상시 노출 + 도메인 툴 %d개 검색(오케스트레이션 어드바이저)%n",
                metaTools.size(), domainTools.size());
        Supplier<ToolCallingAdvisor> loopAdvisorFactory = () -> OrchestrationToolCallingAdvisor.of(
                toolIndex, metaTools, TOOL_SEARCH_MAX_RESULTS, AgentSafety.maxToolRounds(MAX_TOOL_ROUNDS), LOOP_ADVISOR_ORDER);

        return new SpringAIAgent(builder, systemPrompt, domainTools, loopAdvisorFactory, chatMemory, meterRegistry);
    }

    @Bean   // 메타 툴 묶음 - 위임(Task)에 절차, 계획, 질문을 더해 완성
    List<ToolCallback> metaTools(ChatClient.Builder chatClientBuilder) {
        // 위임: 한 작업을 격리된 컨텍스트의 하위 에이전트에 통째로 맡김
        // 하위 에이전트가 쓸 모델, 스킬은 ClaudeSubagentType에서 정함
        var claudeType = ClaudeSubagentType.builder()
                .chatClientBuilder("default", chatClientBuilder.clone())
                // .chatClientBuilder("fast", fastBuilder.clone())
                // .skillsDirectories("src/main/resources/skills")
                .build();
        ToolCallback taskTool = TaskTool.builder()
                .subagentTypes(claudeType)
                .subagentReferences(
                        ClaudeSubagentReferences.fromRootDirectory("src/main/resources/agents"))
                .build();
        // 절차: SKILL.md가 메인 에이전트의 기존 툴을 어떤 순서로 엮을지 알려줌
        ToolCallback skillsTool = SkillsTool.builder()
                .addSkillsDirectory("src/main/resources/skills")
                .build();
        // 계획: 목록이 갱신될 때마다 진행 상황을 콘솔에 출력
        TodoWriteTool todoWriteTool = TodoWriteTool.builder()
                .todoEventHandler(todos -> todos.todos()
                        .forEach(item ->
                                System.out.printf("  [%s] %s%n", item.status(), item.content())))
                .build();
        // 질문: 모호하면 콘솔로 사용자에게 물음
        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
                .questionHandler(new CommandLineQuestionHandler())
                .build();
        List<ToolCallback> metaTools = new ArrayList<>(List.of(taskTool, skillsTool));
        metaTools.addAll(List.of(ToolCallbacks.from(todoWriteTool, askUserQuestionTool)));
        return metaTools;
    }

    /**
     * 코어가 동적 툴 발견을 쓸지 정한다. 도메인 툴(로컬 @Tool + 원격 MCP)이 임계값 이상이고 의미 검색용
     * 벡터스토어가 있으면 켠다. 도메인 툴이 많을 때만 매번 모든 정의를 싣는 대신 검색해서 필요한 것만 올리는
     * 편이 토큰, 지연, 정확도에 유리하기 때문이다. (강화는 이 규칙 대신 늘 {@link OrchestrationToolCallingAdvisor}를
     * 써서, 검색이 표면화하지 못하는 오케스트레이션 메타 툴은 항상 노출하고 도메인 툴만 검색한다.)
     */
    private static boolean shouldUseToolSearch(int toolCount, int threshold, VectorStore vectorStore) {
        return toolCount >= threshold && vectorStore != null;
    }

    /**
     * 툴 호출 루프 어드바이저 팩토리를 만든다. 동적 발견을 쓰면 툴 색인을 <b>한 번</b> 만들어(공유) 그 색인을
     * 참조하는 {@link ToolSearchToolCallingAdvisor}를, 아니면 기본 {@link ToolCallingAdvisor}를 요청마다 새로
     * 만드는 팩토리를 돌려준다. 색인을 공유하는 까닭은 세션별 임베딩을 추적해 재요청 시 직전 임베딩을 정리하기
     * 때문이다(매번 새로 만들면 벡터스토어에 중복이 쌓인다). 루프 어드바이저 자체는 요청마다 새로 만들어 안전
     * 가드 카운터를 요청 사이에 공유하지 않는다.
     */
    private static Supplier<ToolCallingAdvisor> loopAdvisorFactory(boolean useToolSearch, VectorStore vectorStore) {
        if (useToolSearch) {
            ToolIndex toolIndex = new VectorToolIndex(vectorStore);   // 1회 생성, 공유
            return () -> withSafetyGuard(
                    ToolSearchToolCallingAdvisor.builder().toolIndex(toolIndex).maxResults(TOOL_SEARCH_MAX_RESULTS));
        }
        return () -> withSafetyGuard(ToolCallingAdvisor.builder());
    }

    /**
     * 빌더에 두 어드바이저의 공통 설정(요청별 안전 가드 + 순서)을 더해 루프 어드바이저를 완성한다.
     * 팩토리가 요청마다 호출하므로 매 호출이 새 안전 가드(요청별 카운터)를 끼운다. 공통 꼬리를 한곳에 모아 중복을 없앤다.
     */
    private static ToolCallingAdvisor withSafetyGuard(ToolCallingAdvisor.Builder<?> builder) {
        return builder
                .toolExecutionEligibilityChecker(AgentSafety.maxToolRounds(MAX_TOOL_ROUNDS))
                .advisorOrder(LOOP_ADVISOR_ORDER)
                .build();
    }

    /**
     * 로컬 도메인 툴(@Tool) 묶음. 여기서 툴 객체를 하나씩 만들어 ToolCallback 리스트로 모은다.
     * coreAgent, enhancedAgent가 이 리스트를 받아 에이전트에 넘긴다(MCP 툴과 같은 방식).
     *
     * <p>{@code localTools}와 {@code metaTools} 두 {@code List<ToolCallback>} 빈이 공존하면
     * {@code toolCallbackResolver}가 어느 것을 쓸지 모호해져 기동이 실패하므로 이 빈을 {@code @Primary}로 둔다.
     * coreAgent, enhancedAgent는 파라미터 이름으로 주입받아 각자 자기 묶음을 정확히 받는다.
     */
    @Bean
    @Primary
    List<ToolCallback> localTools() {
        return List.of(ToolCallbacks.from(new DateTimeTools(), new CalculatorTools(), new InventoryTools()));
    }

    /** 메인 에이전트의 대화 메모리. MessageChatMemoryAdvisor가 이 저장소로 대화를 기억한다. */
    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    // 동적 툴 발견용 인메모리 벡터 스토어, VectorToolIndex가 이 저장소에 툴 이름, 설명을 임베딩 색인
    @Bean
    VectorStore toolVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

}
