package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.client.McpClientCatalogService;

/**
 * 5장 Client Step 2: MCP 툴, 리소스, 프롬프트, 자동완성 직접 호출.
 */
@Component
@Profile("!server")
@ConditionalOnExpression("'${spring.ai.cli.step:ch5-final}' == 'ch5-client-step2'")
class Ch5Step2_McpPrimitiveCalls implements CommandLineRunner {

    private final McpClientCatalogService catalogService;

    Ch5Step2_McpPrimitiveCalls(McpClientCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Client Step2: MCP 프리미티브 직접 호출 ===");
        System.out.println("\n[Tool: rag_index_summary]");
        System.out.println(catalogService.callTool("rag_index_summary", Map.of()));

        System.out.println("\n[Tool: rag_search_documents]");
        System.out.println(catalogService.callTool("rag_search_documents", Map.of(
                "query", "RAG 운영 정책은 무엇인가요?",
                "topK", 3,
                "category", "tech_docs"
        )));

        System.out.println("\n[Resource: rag://sources]");
        System.out.println(catalogService.readResource("rag://sources"));

        System.out.println("\n[Prompt: rag-grounded-answer]");
        System.out.println(catalogService.getPrompt("rag-grounded-answer", Map.of(
                "question", "긴급 장애 대응 정책은 무엇인가요?",
                "category", "tech_docs"
        )));

        System.out.println("\n[Completion: category=t]");
        System.out.println(catalogService.completeCategory("t"));
    }
}
