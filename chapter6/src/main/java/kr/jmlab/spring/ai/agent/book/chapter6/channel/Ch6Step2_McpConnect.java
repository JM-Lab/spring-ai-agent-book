package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 2 (코어): MCP 서버 운영(Operations) MCP 서버를 연결한다.
 *
 * <p>Step 1의 코어 에이전트는 그대로다. 달라진 것은 운영 서버를 MCP로 연결했다는 점뿐이다. 그러면 서버가
 * 노출한 원격 툴(예: {@code check_stock})이 {@code SyncMcpToolCallbackProvider}를 통해 메인 에이전트의
 * 툴로 합류한다. 로컬 @Tool과 완전히 같은 "툴 호출"이다(단일 툴 인터페이스). 이 러너는 먼저 연결로
 * 합류한 원격 툴 목록을 출력해 확인한 뒤, 운영 재고를 묻는 질문을 던진다.</p>
 *
 * <p>실행 전 다른 터미널에서 운영 서버를 먼저 띄운다: {@code --spring.profiles.active=ops}<br>
 * 실행: {@code --spring.ai.cli.step=ch6-step2}</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step2")
class Ch6Step2_McpConnect implements CommandLineRunner {

    private final SpringAIAgent agent;
    private final ObjectProvider<SyncMcpToolCallbackProvider> mcpToolsProvider;

    Ch6Step2_McpConnect(@Qualifier("coreAgent") SpringAIAgent agent, ObjectProvider<SyncMcpToolCallbackProvider> mcpToolsProvider) {
        this.agent = agent;
        this.mcpToolsProvider = mcpToolsProvider;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step2: 운영 MCP 서버 연결 (원격 툴 합류) ===");

        SyncMcpToolCallbackProvider mcpTools = mcpToolsProvider.getIfAvailable();
        if (mcpTools == null) {
            System.out.println("MCP 서버에 연결되지 않았습니다. 운영 서버(--spring.profiles.active=ops)를 먼저 띄우세요.");
            return;
        }
        System.out.println("[연결로 합류한 원격 툴]");
        for (ToolCallback callback : mcpTools.getToolCallbacks()) {
            System.out.printf("- %s: %s%n",
                    callback.getToolDefinition().name(), callback.getToolDefinition().description());
        }

        String conversationId = UUID.randomUUID().toString();
        String question = "운영 시스템에서 SKU-200의 현재 재고를 확인해줘";
        System.out.println("\n> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
