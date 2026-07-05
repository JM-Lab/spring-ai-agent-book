package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * DefaultContentFormatter 커스텀 설정.
 */
@Component
public class CustomContentFormatExample {

    public List<Document> formatDocuments(List<Document> documents) {
        // 커스텀 포매터 생성
        DefaultContentFormatter formatter = DefaultContentFormatter.builder()
                // 1. 제외할 메타데이터 키 설정(보안/불필요 정보)
                .withExcludedEmbedMetadataKeys("internal_id", "temp_score")
                .withExcludedInferenceMetadataKeys("internal_id")
                // 2. 메타데이터 표시 형식 변경(기본값: "{key}: {value}")
                .withMetadataTemplate("Field [{key}] has value: {value}")
                // 3. 전체 문서 구조 정의(본문 자리에는 {content} 플레이스홀더를 사용)
                // 기본값: "{metadata_string}\n\n{content}"
                .withTextTemplate("--- Document Metadata ---\n{metadata_string}\n\n--- Content ---\n{content}")
                .build();

        // 각 문서에 커스텀 포매터를 직접 지정
        documents.forEach(document -> document.setContentFormatter(formatter));
        return documents;
    }
}
