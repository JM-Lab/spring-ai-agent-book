package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * HTML 읽기 처리.
 *
 * <p>Spring AI 내장 JsoupDocumentReader로 CSS 선택자 기반 본문 추출을 실습한다.</p>
 */
@Component
public class WebPageReader {

    private final Resource resource;

    public WebPageReader(@Value("classpath:data/my-page.html") Resource resource) {
        this.resource = resource;
    }

    /**
     * 기본 설정: article 태그의 p 요소만 추출
     * - 헤더, 네비게이션, 푸터는 제외하고 본문만 가져오기
     */
    public List<Document> readArticleContent() {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .selector("article p")                    // article 내부의 p 태그만 선택
                .charset("UTF-8")
                .includeLinkUrls(true)                    // 링크 URL을 메타데이터에 포함
                .metadataTags(List.of("author", "date"))  // author, date 메타 태그 추출
                .additionalMetadata("source", "my-page.html")
                .build();

        JsoupDocumentReader reader = new JsoupDocumentReader(this.resource, config);
        return reader.read();
    }

    /**
     * 요소별 분할: 각 p 태그를 개별 Document로 생성
     * - 문단별로 독립적인 검색이 필요할 때 사용
     */
    public List<Document> readByParagraph() {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .selector("article p")
                .groupByElement(true)                     // 각 p 태그마다 별도 Document 생성
                .includeLinkUrls(false)
                .build();

        JsoupDocumentReader reader = new JsoupDocumentReader(this.resource, config);
        return reader.read();
    }

    /**
     * 전체 본문 추출: body 태그 전체를 하나의 Document로
     */
    public List<Document> readFullBody() {
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                .allElements(true)                        // selector 무시하고 body 전체 추출
                .charset("UTF-8")
                .build();

        JsoupDocumentReader reader = new JsoupDocumentReader(this.resource, config);
        return reader.read();
    }

}
