package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

import java.util.Arrays;
import java.util.Comparator;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.OperationsMcpTools;

/**
 * 운영(Operations) MCP 서버 (Step 2~3에서 연결).
 *
 * <p>{@code ops} 프로파일로 띄우는 MCP 서버다(포트 8085). 주문, 재고 조회/변경 능력을 코어 @McpTool로 노출한다.
 * 그중 {@code place_purchase_order}는 비가역 작업이라 실행 직전 server-side Elicitation으로 승인을 요청한다.
 * 이 러너는 기동 시 노출 툴을 출력만 하고, 이후 웹 서버로 떠 있으면서 클라이언트의 MCP 연결을 받는다.</p>
 *
 * <p>실행: {@code --spring.profiles.active=ops}</p>
 */
@Component
@Profile("ops")
class Ch6OpsMcpServer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] 운영(Operations) MCP 서버, 포트 8085 ===");
        System.out.println("[노출 MCP 툴]");
        Arrays.stream(OperationsMcpTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .sorted(Comparator.comparing(McpTool::name))
                .forEach(tool -> System.out.printf("- %s: %s%n", tool.name(), tool.description()));
        System.out.println("\n클라이언트의 MCP 연결을 기다립니다. (Ctrl+C로 종료)");
    }
}
