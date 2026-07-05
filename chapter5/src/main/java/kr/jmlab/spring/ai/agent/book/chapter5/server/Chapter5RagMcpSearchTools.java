package kr.jmlab.spring.ai.agent.book.chapter5.server;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.support.RagDocumentLoader.IndexReport;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagMcpKnowledgeBase;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagSearchService;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagSearchService.RagSearchResponse;

/**
 * 클라이언트 측 답변 패턴용 MCP 툴: 검색만 수행하고 결과 문서를 그대로 반환한다.
 *
 * <p>이 클래스의 툴은 LLM 호출 없이 벡터 검색 결과만 클라이언트에 돌려준다.
 * 클라이언트의 ChatClient 가 sources 를 근거로 답변을 직접 구성한다.</p>
 */
@Component
@Profile("server")
public class Chapter5RagMcpSearchTools {

    private final RagMcpKnowledgeBase knowledgeBase;
    private final RagSearchService searchService;

    public Chapter5RagMcpSearchTools(RagMcpKnowledgeBase knowledgeBase, RagSearchService searchService) {
        this.knowledgeBase = knowledgeBase;
        this.searchService = searchService;
    }

    @McpTool(
            name = "rag_index_summary",
            description = "MCP 서버의 RAG 문서 ETL과 벡터 인덱싱 결과를 반환합니다.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "RAG 인덱스 요약",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public IndexReport indexSummary() {
        return knowledgeBase.getIndexReport();
    }

    @McpTool(
            name = "rag_search_documents",
            description = "질문과 관련된 RAG 문서 조각을 벡터 검색으로 찾아 출처, 점수, 요약 텍스트를 반환합니다.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    title = "RAG 문서 검색",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public RagSearchResponse searchDocuments(
            @McpToolParam(description = "검색할 질문 또는 키워드", required = true)
            String query,
            @McpToolParam(description = "가져올 문서 수. 기본값 5, 최대 10", required = false)
            Integer topK,
            @McpToolParam(description = "선택 필터. tech_docs, product_catalog, web_docs 중 하나", required = false)
            String category) {
        return searchService.search(query, topK, category);
    }
}
