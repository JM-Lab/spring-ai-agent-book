package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.client.McpClientCatalogService;

/**
 * 5장 Client Step 4: 클라이언트 필터링, 이름 정책, ToolContext 메타데이터 변환 확인.
 */
@Component
@Profile("!server")
@ConditionalOnExpression("'${spring.ai.cli.step:ch5-final}' == 'ch5-client-step4'")
class Ch5Step4_McpClientPolicy implements CommandLineRunner {

    private final McpClientCatalogService catalogService;
    private final ToolContextToMcpMetaConverter metaConverter;

    Ch5Step4_McpClientPolicy(
            McpClientCatalogService catalogService,
            ToolContextToMcpMetaConverter metaConverter) {
        this.catalogService = catalogService;
        this.metaConverter = metaConverter;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Client Step4: MCP 클라이언트 정책 ===");
        System.out.println("[Allowed ToolCallbacks]");
        System.out.println(catalogService.listToolCallbacks());

        Map<String, Object> metadata = metaConverter.convert(new ToolContext(Map.of(
                "userId", "policy-user",
                "conversationId", "policy-conversation",
                "clientSession", "ch5-client-step4",
                "rawSecret", "never-forward"
        )));

        System.out.println("\n[Converted ToolContext -> MCP _meta]");
        System.out.println(metadata);
    }
}
