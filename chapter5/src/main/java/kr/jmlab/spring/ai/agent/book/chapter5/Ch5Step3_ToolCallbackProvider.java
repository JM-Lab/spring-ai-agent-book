package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.client.McpClientCatalogService;

/**
 * 5장 Client Step 3: MCP 툴을 Spring AI ToolCallback으로 변환해 실행.
 */
@Component
@Profile("!server")
@ConditionalOnExpression("'${spring.ai.cli.step:ch5-final}' == 'ch5-client-step3'")
class Ch5Step3_ToolCallbackProvider implements CommandLineRunner {

    private final McpClientCatalogService catalogService;

    Ch5Step3_ToolCallbackProvider(McpClientCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Client Step3: SyncMcpToolCallbackProvider ===");
        System.out.println(catalogService.listToolCallbacks());

        ToolCallback callback = catalogService.toolCallback("rag_search_documents");
        String result = callback.call("""
                {"query": "개인 정보가 포함된 문서는 어떻게 처리해야 하나요?", "topK": 3, "category": "tech_docs"}
                """, new ToolContext(Map.of(
                "userId", "step3-user",
                "conversationId", "step3-conversation",
                "clientSession", "ch5-client-step3"
        )));

        System.out.println("\n[Direct ToolCallback Result]");
        System.out.println(result);
    }
}
