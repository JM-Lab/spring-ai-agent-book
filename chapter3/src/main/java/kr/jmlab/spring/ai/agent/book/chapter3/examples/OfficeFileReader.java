package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tika 기반 범용 문서 읽기.
 */
@Component
public class OfficeFileReader {

    /**
     * 기본 사용법: 파일 형식 자동 감지 및 텍스트 추출
     * - Tika가 파일 헤더를 분석하여 형식을 자동으로 감지하고 텍스트를 추출
     */
    public List<Document> readOfficeFile(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        List<Document> documents = reader.read();

        // 원본 파일명을 메타데이터로 보존하여 추적 가능하게 함
        documents.forEach(doc ->
                doc.getMetadata().put("sourceFilename", resource.getFilename())
        );

        return documents;
    }

    /**
     * 형식별 메타데이터 활용 예제
     * - 추출된 문서의 타입에 따라 다른 카테고리를 부여
     */
    public List<Document> readWithCategorization(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();

        String filename = resource.getFilename();
        String extension = StringUtils.getFilenameExtension(filename);

        documents.forEach(doc -> {
            if ("pdf".equalsIgnoreCase(extension)) {
                doc.getMetadata().put("docType", "report");
            } else if ("xlsx".equalsIgnoreCase(extension)) {
                doc.getMetadata().put("docType", "data_sheet");
            } else if ("pptx".equalsIgnoreCase(extension)) {
                doc.getMetadata().put("docType", "presentation");
            }
        });

        return documents;
    }
}
