package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.KnowledgeMcpTools;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.KnowledgeBase;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagAnswerService;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagAnswerService.RagAnswerResponse;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagAnswerService.RagAnswerSource;
import kr.jmlab.spring.ai.agent.book.chapter6.capability.remote.RagDocumentLoader.IndexReport;

/**
 * 지식(Knowledge) MCP 서버, 하위 에이전트 툴 (Step 4에서 연결).
 *
 * <p>{@code knowledge} 프로파일로 띄우는 MCP 서버다(포트 8086). 기동 시 RAG 지식 베이스를 인덱싱하고
 * {@code rag_answer_question} 툴을 MCP로 노출한다. 이 툴은 단순 함수가 아니라, 서버 내부 ChatClient 루프가
 * 검색에서 답변까지 수행하는 '에이전트가 든 툴(agent-as-a-tool)'이다. 클라이언트가 보기에는 외부 툴
 * 하나를 부른 것이지만, 그 경계 너머에서는 독립적으로 사고하는 하위 에이전트가 일한다.</p>
 *
 * <p>실행: {@code --spring.profiles.active=knowledge}</p>
 */
@Component
@Profile("knowledge")
class Ch6KnowledgeMcpServer implements CommandLineRunner {

    private final KnowledgeBase knowledgeBase;
    private final RagAnswerService answerService;

    Ch6KnowledgeMcpServer(KnowledgeBase knowledgeBase, RagAnswerService answerService) {
        this.knowledgeBase = knowledgeBase;
        this.answerService = answerService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] 지식(Knowledge) MCP 서버, 포트 8086 (agent-as-a-tool) ===");

        IndexReport report = knowledgeBase.getIndexReport();
        System.out.printf("문서 디렉터리: %s%n", report.documentsDirectory());
        System.out.printf("원천 문서: %d, 마스킹 후: %d, 청크: %d%n",
                report.rawDocumentCount(), report.maskedDocumentCount(), report.chunkCount());

        System.out.println("\n[노출 MCP 툴, 하위 에이전트가 든 툴]");
        Arrays.stream(KnowledgeMcpTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .sorted(Comparator.comparing(McpTool::name))
                .forEach(tool -> System.out.printf("- %s: %s%n", tool.name(), tool.description()));

        System.out.println("\n[샘플 호출] 클라이언트가 이 툴을 부르면 서버 내부 에이전트가 스스로 답한다:");
        RagAnswerResponse response = answerService.answer(
                "비밀번호는 얼마나 자주 변경해야 하나요?", 3, null, Map.of("demo", "ch6-knowledge"));
        System.out.println("Q: " + response.question());
        System.out.println("A: " + response.answer());
        System.out.println("근거 문서: " + response.sources().stream()
                .map(RagAnswerSource::source).distinct().toList());

        System.out.println("\n클라이언트의 MCP 연결을 기다립니다. (Ctrl+C로 종료)");
    }
}
