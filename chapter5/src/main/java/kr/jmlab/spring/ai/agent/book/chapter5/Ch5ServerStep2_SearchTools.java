package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Arrays;
import java.util.Comparator;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.server.Chapter5RagMcpSearchTools;

/**
 * 5장 Server Step 2: 검색 전용 패턴용 MCP Tool 확인.
 *
 * <p>클라이언트 측 답변 패턴에 사용하는 Chapter5RagMcpSearchTools 의 메서드 명세를 출력한다.</p>
 */
@Component
@Profile("server")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch5-server-step2")
class Ch5ServerStep2_SearchTools implements CommandLineRunner {

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Server Step2: 검색 전용 패턴 MCP 툴 공개 ===");
        System.out.println("Pattern: 클라이언트 측 답변 (rag_search_documents + 클라이언트 LLM)");
        System.out.println("Endpoint: http://localhost:8085/mcp");

        System.out.println("\n[Search Tools]");
        Arrays.stream(Chapter5RagMcpSearchTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .sorted(Comparator.comparing(McpTool::name))
                .forEach(tool -> System.out.printf("- %s: %s%n", tool.name(), tool.description()));
    }
}
