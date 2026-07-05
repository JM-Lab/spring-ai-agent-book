package kr.jmlab.spring.ai.agent.book.chapter5.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 자동 구성된 MCP 클라이언트와 ToolCallbackProvider를 CLI에서 보기 좋게 사용한다.
 */
@Service
@Profile("!server")
public class McpClientCatalogService {

    private final List<McpSyncClient> mcpClients;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    public McpClientCatalogService(
            List<McpSyncClient> mcpClients,
            SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.mcpClients = mcpClients;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public String serverSummary() {
        McpSyncClient client = primaryClient();
        McpSchema.Implementation serverInfo = client.getServerInfo();
        McpSchema.ServerCapabilities capabilities = client.getServerCapabilities();
        return """
                Server: %s %s
                Instructions: %s
                Capabilities: %s
                """.formatted(
                serverInfo.name(),
                serverInfo.version(),
                client.getServerInstructions(),
                capabilities);
    }

    public String listTools() {
        return primaryClient().listTools().tools().stream()
                .map(tool -> "- %s: %s".formatted(tool.name(), tool.description()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("노출된 MCP 툴이 없습니다.");
    }

    public String listToolCallbacks() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        if (callbacks.length == 0) {
            return "등록된 ToolCallback이 없습니다.";
        }
        return Arrays.stream(callbacks)
                .map(callback -> "- %s: %s".formatted(
                        callback.getToolDefinition().name(),
                        callback.getToolDefinition().description()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("등록된 ToolCallback이 없습니다.");
    }

    public String listResources() {
        return primaryClient().listResources().resources().stream()
                .map(resource -> "- %s (%s): %s".formatted(
                        resource.uri(),
                        resource.mimeType(),
                        resource.description()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("노출된 MCP 리소스가 없습니다.");
    }

    public String readResource(String uri) {
        McpSchema.ReadResourceResult result = primaryClient().readResource(new McpSchema.ReadResourceRequest(uri));
        return result.contents().stream()
                .map(this::contentToText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("리소스 내용이 비어 있습니다.");
    }

    public String listPrompts() {
        return primaryClient().listPrompts().prompts().stream()
                .map(prompt -> "- %s: %s".formatted(prompt.name(), prompt.description()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("노출된 MCP 프롬프트가 없습니다.");
    }

    public String getPrompt(String name, Map<String, Object> arguments) {
        McpSchema.GetPromptResult result = primaryClient().getPrompt(new McpSchema.GetPromptRequest(name, arguments));
        return result.messages().stream()
                .map(message -> contentToText(message.content()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("프롬프트 메시지가 비어 있습니다.");
    }

    public String completeCategory(String value) {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("rag-grounded-answer"),
                new McpSchema.CompleteRequest.CompleteArgument("category", value));
        McpSchema.CompleteResult result = primaryClient().completeCompletion(request);
        return String.join(", ", result.completion().values());
    }

    public String callTool(String name, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = primaryClient().callTool(new McpSchema.CallToolRequest(name, arguments));
        return result.content().stream()
                .map(this::contentToText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("툴 실행 결과가 비어 있습니다.");
    }

    /**
     * 서버 측 답변 패턴을 명시적으로 호출한다. rag_answer_question 툴을 직접 실행해 서버 내부 LLM 이 생성한 답변을 그대로 받는다.
     */
    public String answerDirect(String question) {
        return callTool("rag_answer_question", Map.of(
                "question", question,
                "topK", 5));
    }

    public ToolCallback toolCallback(String name) {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .filter(callback -> callback.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ToolCallback을 찾을 수 없습니다: " + name));
    }

    private McpSyncClient primaryClient() {
        if (mcpClients.isEmpty()) {
            throw new IllegalStateException("연결된 MCP 서버가 없습니다.");
        }
        return mcpClients.getFirst();
    }

    private String contentToText(McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        return String.valueOf(content);
    }

    private String contentToText(McpSchema.ResourceContents content) {
        if (content instanceof McpSchema.TextResourceContents textContent) {
            return textContent.text();
        }
        return String.valueOf(content);
    }
}
