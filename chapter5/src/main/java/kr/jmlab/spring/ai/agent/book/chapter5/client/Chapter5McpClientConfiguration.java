package kr.jmlab.spring.ai.agent.book.chapter5.client;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 5장 최종 CLI에서 사용하는 MCP 클라이언트 확장 지점.
 */
@Configuration
@Profile("!server")
public class Chapter5McpClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Chapter5McpClientConfiguration.class);
    private static final Set<String> ALLOWED_META_KEYS = Set.of(
            "userId",
            "conversationId",
            "clientSession",
            "progressToken"
    );

    @Bean
    McpClientCustomizer<McpClient.SyncSpec> chapter5McpSyncClientCustomizer() {
        return (serverConfigurationName, spec) -> spec
                .requestTimeout(Duration.ofSeconds(30))
                .roots(new McpSchema.Root(workspaceRoot(), "chapter5 workspace"))
                .progressConsumer(progress -> logger.info("[{}] MCP progress: {} / {} {}",
                        serverConfigurationName,
                        progress.progress(),
                        progress.total(),
                        progress.message()))
                .loggingConsumer(logMessage -> logger.info("[{}] MCP log [{}] {}",
                        serverConfigurationName,
                        logMessage.level(),
                        logMessage.data()));
    }

    @Bean
    McpToolFilter chapter5McpToolFilter() {
        return (connectionInfo, tool) -> tool.name().startsWith("rag_")
                && (tool.description() == null || !tool.description().contains("experimental"));
    }

    @Bean
    McpToolNamePrefixGenerator chapter5McpToolNamePrefixGenerator() {
        return McpToolNamePrefixGenerator.noPrefix();
    }

    @Bean
    ToolContextToMcpMetaConverter chapter5ToolContextToMcpMetaConverter() {
        return this::toMcpMeta;
    }

    public Map<String, Object> toMcpMeta(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return Map.of();
        }

        Map<String, Object> metadata = new HashMap<>();
        toolContext.getContext().forEach((key, value) -> {
            if (value != null && ALLOWED_META_KEYS.contains(key)) {
                metadata.put(key, value);
            }
        });
        metadata.put("source", "chapter5-cli");
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }

    private String workspaceRoot() {
        // 공백, 한글 경로도 안전하게 URI로 변환하기 위해 Path.toUri()를 사용한다.
        return Path.of(".").toAbsolutePath().normalize().toUri().toString();
    }
}
