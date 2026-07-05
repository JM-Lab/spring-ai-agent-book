package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.stereotype.Component;

/**
 * SummaryMetadataEnricher 기본 사용.
 */
@Component
public class SummaryEnricherExample {

    private final ChatModel chatModel;

    public SummaryEnricherExample(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public List<Document> enrich(List<Document> docs) {

        // 1. Enricher 생성
        // 필요한 요약 종류를 리스트로 전달
        // 현재 요약만 필요하면 List.of(SummaryMetadataEnricher.SummaryType.CURRENT)만 사용
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(
                chatModel,
                List.of(
                        SummaryMetadataEnricher.SummaryType.PREVIOUS, // -> 'prev_section_summary' 생성
                        SummaryMetadataEnricher.SummaryType.CURRENT,  // -> 'section_summary' 생성
                        SummaryMetadataEnricher.SummaryType.NEXT      // -> 'next_section_summary' 생성
                )
        );

        // 2. 문서 리스트에 적용(요약 생성 및 메타데이터 추가)
        List<Document> enrichedDocs = enricher.transform(docs);

        // 3. 결과 확인(로그 출력)
        printSummaries(enrichedDocs);

        return enrichedDocs;
    }

    private void printSummaries(List<Document> enrichedDocs) {
        for (int i = 0; i < enrichedDocs.size(); i++) {
            Document doc = enrichedDocs.get(i);
            System.out.println("\n=== 문서 조각 " + (i + 1) + " ===");

            // 위에서 활성화한 옵션에 따라 메타데이터 키가 생성
            System.out.println("[Current]: " + doc.getMetadata().get("section_summary"));

            // 첫 번째 문서는 이전(Prev) 요약이 null
            System.out.println("[Prev]   : " + doc.getMetadata().get("prev_section_summary"));

            // 마지막 문서는 다음(Next) 요약이 null
            System.out.println("[Next]   : " + doc.getMetadata().get("next_section_summary"));
        }
    }
}
