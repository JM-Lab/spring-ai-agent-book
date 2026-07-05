package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Comparator;
import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 5장 Server Step 4: RAG MCP 서버가 공개하는 Resource, Prompt, Completion 확인.
 */
@Component
@Profile("server")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch5-server-step4")
class Ch5ServerStep4_McpServerPrimitives implements CommandLineRunner {

    private final List<McpServerFeatures.SyncResourceSpecification> resources;
    private final List<McpServerFeatures.SyncPromptSpecification> prompts;
    private final List<McpServerFeatures.SyncCompletionSpecification> completions;

    Ch5ServerStep4_McpServerPrimitives(
            // Spring AI MCP 서버 자동 구성이 동일 타입의 집계 빈을 만들기 때문에
            // Chapter5RagMcpPrimitiveConfig 에서 직접 등록한 빈을 @Qualifier 로 명시한다.
            @Qualifier("ragResources") List<McpServerFeatures.SyncResourceSpecification> resources,
            @Qualifier("ragPrompts") List<McpServerFeatures.SyncPromptSpecification> prompts,
            @Qualifier("ragCompletions") List<McpServerFeatures.SyncCompletionSpecification> completions) {
        this.resources = resources;
        this.prompts = prompts;
        this.completions = completions;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Server Step4: Resource, Prompt, Completion 공개 ===");
        System.out.println("Endpoint: http://localhost:8085/mcp");

        System.out.println("\n[Resources]");
        resources.stream()
                .map(McpServerFeatures.SyncResourceSpecification::resource)
                .sorted(Comparator.comparing(McpSchema.Resource::uri))
                .forEach(resource -> System.out.printf("- %s (%s): %s%n",
                        resource.uri(),
                        resource.mimeType(),
                        resource.description()));

        System.out.println("\n[Prompts]");
        prompts.stream()
                .map(McpServerFeatures.SyncPromptSpecification::prompt)
                .sorted(Comparator.comparing(McpSchema.Prompt::name))
                .forEach(prompt -> System.out.printf("- %s: %s%n",
                        prompt.name(),
                        prompt.description()));

        System.out.println("\n[Completions]");
        completions.stream()
                .map(McpServerFeatures.SyncCompletionSpecification::referenceKey)
                .map(reference -> reference instanceof McpSchema.PromptReference promptReference
                        ? "prompt:" + promptReference.name()
                        : reference.toString())
                .sorted()
                .forEach(reference -> System.out.printf("- %s%n", reference));
    }
}
