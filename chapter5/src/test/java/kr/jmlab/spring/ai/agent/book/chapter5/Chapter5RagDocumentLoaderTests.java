package kr.jmlab.spring.ai.agent.book.chapter5;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import kr.jmlab.spring.ai.agent.book.chapter5.client.Chapter5McpClientConfiguration;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagDocumentLoader;

/**
 * 5장 RAG MCP 서버의 문서 ETL과 클라이언트 메타데이터 정책을 모델 호출 없이 검증한다.
 */
class Chapter5RagDocumentLoaderTests {

    @Test
    @DisplayName("3장 샘플 문서를 MCP RAG 서버용 문서 청크로 준비한다")
    void prepareChapter3DocumentsForRagMcpServer() {
        RagDocumentLoader loader = new RagDocumentLoader(new ObjectMapper());

        RagDocumentLoader.PreparedDocuments prepared = loader.prepare(Path.of("src/main/resources/data"));

        assertThat(prepared.report().sourceDocuments())
                .extracting(RagDocumentLoader.SourceDocument::filename)
                .contains("policy-docs.txt", "bikes.json", "spring-boot-guide.md", "my-page.html");
        assertThat(prepared.report().rawDocumentCount()).isGreaterThanOrEqualTo(5);
        assertThat(prepared.report().chunkCount()).isGreaterThan(0);
        assertThat(prepared.chunks())
                .anySatisfy(document -> assertThat(document.getMetadata())
                        .containsEntry("category", "tech_docs")
                        .containsEntry("isActive", true));
    }

    @Test
    @DisplayName("문서 준비 과정에서 이메일과 전화번호를 마스킹한다")
    void prepareMasksSensitiveText() {
        RagDocumentLoader loader = new RagDocumentLoader(new ObjectMapper());

        RagDocumentLoader.PreparedDocuments prepared = loader.prepare(Path.of("src/main/resources/data"));
        String joinedText = prepared.chunks().stream()
                .map(document -> document.getText())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        assertThat(joinedText)
                .contains("[EMAIL]")
                .contains("[PHONE]")
                .doesNotContain("security@example.com")
                .doesNotContain("010-1234-5678");
    }

    @Test
    @DisplayName("ToolContextToMcpMetaConverter 는 허용된 실행 문맥만 MCP _meta 로 전달한다")
    void mcpMetadataConverterWhitelistsContext() {
        Chapter5McpClientConfiguration configuration = new Chapter5McpClientConfiguration();

        Map<String, Object> metadata = configuration.toMcpMeta(new ToolContext(Map.of(
                "userId", "tester",
                "conversationId", "conversation-1",
                "rawSecret", "must-not-leak"
        )));

        assertThat(metadata)
                .containsEntry("userId", "tester")
                .containsEntry("conversationId", "conversation-1")
                .containsEntry("source", "chapter5-cli")
                .doesNotContainKey("rawSecret");
    }
}
