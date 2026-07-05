package kr.jmlab.spring.ai.agent.book.chapter5.server;

import java.util.Map;

import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.support.RagAnswerService;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagAnswerService.RagAnswerResponse;

/**
 * 서버 측 답변 패턴용 MCP 툴: 검색과 LLM 답변 생성을 서버 내부에서 모두 수행한다.
 *
 * <p>이 클래스의 툴은 RagAnswerService 안의 ChatClient 로 답변까지 만든 뒤 완성된 텍스트를 반환한다.
 * 클라이언트는 별도 답변 생성 없이 결과를 그대로 사용자에게 노출할 수 있다.</p>
 *
 * <p>McpMeta 파라미터는 JSON 스키마 대상에서 제외되는 특수 파라미터로,
 * 클라이언트가 전달한 실행 문맥(userId, conversationId, clientSession 등)을 서버에서 안전하게 수신한다.</p>
 */
@Component
@Profile("server")
public class Chapter5RagMcpAnswerTools {

    private final RagAnswerService answerService;

    public Chapter5RagMcpAnswerTools(RagAnswerService answerService) {
        this.answerService = answerService;
    }

    @McpTool(
            name = "rag_answer_question",
            description = "RAG MCP 서버가 문서를 검색한 뒤 검색 문서만 근거로 질문에 답합니다.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "RAG 근거 기반 답변",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = false,
                    openWorldHint = false))
    public RagAnswerResponse answerQuestion(
            @McpToolParam(description = "근거 기반으로 답할 사용자 질문", required = true)
            String question,
            @McpToolParam(description = "답변에 사용할 검색 문서 수. 기본값 5, 최대 10", required = false)
            Integer topK,
            @McpToolParam(description = "선택 필터. tech_docs, product_catalog, web_docs 중 하나", required = false)
            String category,
            McpMeta meta) {
        return answerService.answer(question, topK, category,
                meta == null ? Map.of() : meta.meta());
    }
}
