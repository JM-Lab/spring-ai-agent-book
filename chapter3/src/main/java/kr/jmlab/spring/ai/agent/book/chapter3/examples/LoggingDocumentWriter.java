package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.stereotype.Component;

/**
 * 로거를 활용한 사용자 정의 DocumentWriter.
 */
@Component
public class LoggingDocumentWriter implements DocumentWriter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDocumentWriter.class);

    @Override
    public void accept(List<Document> documents) {
        logger.info("=== Custom Document Writer 시작 ===");

        for (Document doc : documents) {
            // 문서의 ID와 길이 정보만 디버그 레벨로 기록
            logger.debug("Document ID: {}, Content Length: {}", doc.getId(), doc.getText().length());
        }

        logger.info("=== 총 {}건 처리 완료 ===", documents.size());
    }
}
