package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 마크다운 읽기 처리.
 *
 * <p>Spring AI 내장 MarkdownDocumentReader로 코드 블록/인용문 분리 여부를 비교한다.</p>
 */
@Component
public class MarkdownDocsReader {

    private final Resource resource;

    public MarkdownDocsReader(@Value("classpath:data/spring-boot-guide.md") Resource resource) {
        this.resource = resource;
    }

    /**
     * 코드 블록과 인용문을 별도 Document로 분리하는 설정
     * - 코드 예제를 독립적으로 검색할 수 있어 기술 문서 처리에 유리
     */
    public List<Document> loadMarkdown() {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)  // '---'로 문서 분할
                .withIncludeCodeBlock(false)             // 코드 블록을 별도 Document로 분리
                .withIncludeBlockquote(false)            // 인용문을 별도 Document로 분리
                .withAdditionalMetadata("filename", "spring-boot-guide.md")
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(this.resource, config);
        return reader.read();
    }

    /**
     * 코드와 설명을 하나의 Document로 유지하는 설정
     * - 문맥이 중요할 때 사용
     */
    public List<Document> loadMarkdownIntegrated() {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)  // '---'로 문서 분할
                .withIncludeCodeBlock(true)              // 코드 블록을 본문에 포함
                .withIncludeBlockquote(true)             // 인용문을 본문에 포함
                .withAdditionalMetadata("docType", "techGuide")
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(this.resource, config);
        return reader.read();
    }
}
