package kr.jmlab.spring.ai.agent.book.chapter5;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.client.McpClientCatalogService;

/**
 * 5장 Client Step 1: MCP 서버 연결과 프리미티브 목록 확인.
 */
@Component
@Profile("!server")
@ConditionalOnExpression("'${spring.ai.cli.step:ch5-final}' == 'ch5-client-step1'")
class Ch5Step1_McpDiscovery implements CommandLineRunner {

    private final McpClientCatalogService catalogService;

    Ch5Step1_McpDiscovery(McpClientCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Client Step1: MCP 연결과 프리미티브 목록 ===");
        System.out.println(catalogService.serverSummary());
        System.out.println("[Tools]");
        System.out.println(catalogService.listTools());
        System.out.println("\n[Resources]");
        System.out.println(catalogService.listResources());
        System.out.println("\n[Prompts]");
        System.out.println(catalogService.listPrompts());
    }
}
