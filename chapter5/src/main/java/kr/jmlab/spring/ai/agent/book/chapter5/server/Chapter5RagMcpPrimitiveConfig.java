package kr.jmlab.spring.ai.agent.book.chapter5.server;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import kr.jmlab.spring.ai.agent.book.chapter5.support.RagDocumentLoader.IndexReport;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagMcpKnowledgeBase;

/**
 * RAG MCP 서버의 Resource, Prompt, Completion 프리미티브.
 */
@Configuration
@Profile("server")
public class Chapter5RagMcpPrimitiveConfig {

    @Bean
    List<McpServerFeatures.SyncResourceSpecification> ragResources(RagMcpKnowledgeBase knowledgeBase) {
        McpSchema.Resource sources = McpSchema.Resource.builder()
                .uri("rag://sources")
                .name("rag-sources")
                .title("RAG 원천 문서 목록")
                .description("MCP RAG 서버가 인덱싱한 원천 문서와 청크 수를 제공합니다.")
                .mimeType("text/plain")
                .build();

        McpSchema.Resource pipeline = McpSchema.Resource.builder()
                .uri("rag://pipeline")
                .name("rag-pipeline")
                .title("RAG MCP 파이프라인")
                .description("문서 ETL, 벡터 검색, 근거 기반 답변 생성 흐름을 설명합니다.")
                .mimeType("text/plain")
                .build();

        return List.of(
                new McpServerFeatures.SyncResourceSpecification(sources,
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "text/plain",
                                        formatIndexReport(knowledgeBase.getIndexReport()))))),
                new McpServerFeatures.SyncResourceSpecification(pipeline,
                        (exchange, request) -> new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(
                                        request.uri(),
                                        "text/plain",
                                        knowledgeBase.pipelineDescription()))))
        );
    }

    @Bean
    List<McpServerFeatures.SyncPromptSpecification> ragPrompts() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "rag-grounded-answer",
                "RAG 근거 기반 답변",
                "RAG MCP 서버의 문서 검색 도구를 사용해 답변할 때 적용할 프롬프트입니다.",
                List.of(
                        new McpSchema.PromptArgument("question", "사용자 질문", true),
                        new McpSchema.PromptArgument("category", "tech_docs, product_catalog, web_docs 중 선택", false)
                ),
                Map.of("chapter", "5", "server", "rag-mcp"));

        return List.of(new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            String question = String.valueOf(args.getOrDefault("question", "RAG 질문"));
            String category = String.valueOf(args.getOrDefault("category", ""));
            String text = """
                    rag_answer_question 도구를 호출해 아래 질문에 답하세요.
                    - question: %s
                    - category: %s

                    답변에서는 도구가 반환한 answer와 sources를 모두 확인하고,
                    문서에 없는 내용은 추측하지 마세요.
                    """.formatted(question, category.isBlank() ? "전체" : category);

            return new McpSchema.GetPromptResult(
                    "RAG MCP 근거 기반 답변 프롬프트",
                    List.of(new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(text))));
        }));
    }

    @Bean
    List<McpServerFeatures.SyncCompletionSpecification> ragCompletions(RagMcpKnowledgeBase knowledgeBase) {
        McpSchema.PromptReference reference = new McpSchema.PromptReference("rag-grounded-answer");

        return List.of(new McpServerFeatures.SyncCompletionSpecification(reference, (exchange, request) -> {
            String value = request.argument().value();
            List<String> values = knowledgeBase.categorySuggestions(value);
            return new McpSchema.CompleteResult(
                    new McpSchema.CompleteResult.CompleteCompletion(values, values.size(), false));
        }));
    }

    private String formatIndexReport(IndexReport report) {
        String sources = report.sourceDocuments().stream()
                .map(source -> "- %s (%s): %d documents".formatted(
                        source.filename(),
                        source.sourceType(),
                        source.documentCount()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- no source documents");

        return """
                documentsDirectory: %s
                rawDocumentCount: %d
                maskedDocumentCount: %d
                chunkCount: %d

                sources:
                %s
                """.formatted(
                report.documentsDirectory(),
                report.rawDocumentCount(),
                report.maskedDocumentCount(),
                report.chunkCount(),
                sources);
    }
}
