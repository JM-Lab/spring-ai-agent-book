package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Arrays;
import java.util.Comparator;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.server.Chapter5RagMcpAnswerTools;

/**
 * 5장 Server Step 3: 서버 측 답변 패턴용 MCP Tool 확인.
 *
 * <p>서버 측 답변 패턴에 사용하는 Chapter5RagMcpAnswerTools 의 메서드 명세를 출력한다.
 * 이 패턴은 서버 내부의 ChatClient 가 답변까지 생성한 뒤 완성된 텍스트를 반환한다.</p>
 */
@Component
@Profile("server")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch5-server-step3")
class Ch5ServerStep3_AnswerTools implements CommandLineRunner {

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Server Step3: 서버 측 답변 패턴 MCP 툴 공개 ===");
        System.out.println("Pattern: 서버 측 답변 (rag_answer_question, 서버 내부 ChatClient 가 답변 생성)");
        System.out.println("Endpoint: http://localhost:8085/mcp");

        System.out.println("\n[Answer Tools]");
        Arrays.stream(Chapter5RagMcpAnswerTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .sorted(Comparator.comparing(McpTool::name))
                .forEach(tool -> System.out.printf("- %s: %s%n", tool.name(), tool.description()));

        System.out.println("\n[Special Parameter]");
        System.out.println("- McpMeta: 클라이언트가 전달한 실행 문맥(userId, conversationId 등)을 서버에서 수신");
    }
}
