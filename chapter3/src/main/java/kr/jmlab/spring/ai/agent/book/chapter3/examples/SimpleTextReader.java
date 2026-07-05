package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 텍스트 파일 읽기.
 */
@Component
public class SimpleTextReader {

    private final Resource resource;

    public SimpleTextReader(@Value("classpath:data/policy-docs.txt") Resource resource) {
        this.resource = resource;
    }

    /**
     * 기본 사용법: UTF-8 인코딩으로 텍스트 파일 읽기
     */
    public List<Document> readTextFile() {
        TextReader reader = new TextReader(this.resource);

        // 인코딩 설정(기본값: UTF-8)
        reader.setCharset(StandardCharsets.UTF_8);

        return reader.read();
    }

    /**
     * 커스텀 메타데이터 추가
     * - 데이터 수집 시점, 출처 등의 정보를 메타데이터로 보존
     */
    public List<Document> readWithMetadata() {
        TextReader reader = new TextReader(this.resource);

        // 커스텀 메타데이터 추가
        reader.getCustomMetadata().put("ingestedAt", LocalDate.now().toString());
        reader.getCustomMetadata().put("category", "tech_docs");
        reader.getCustomMetadata().put("version", "1.0");

        return reader.read();
    }

}
