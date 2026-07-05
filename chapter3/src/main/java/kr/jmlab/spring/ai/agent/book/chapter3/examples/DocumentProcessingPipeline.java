package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

/**
 * DocumentTransformer 통합 파이프라인 서비스.
 */
@Service
public class DocumentProcessingPipeline {

    // 커스텀 컴포넌트 주입
    private final PiiMaskingTransformer piiMaskingTransformer;
    private final CustomKeywordEnricher keywordEnricher;
    private final CustomSummaryEnricher summaryEnricher;

    public DocumentProcessingPipeline(
            PiiMaskingTransformer piiMaskingTransformer,
            CustomKeywordEnricher keywordEnricher,
            CustomSummaryEnricher summaryEnricher) {
        this.piiMaskingTransformer = piiMaskingTransformer;
        this.keywordEnricher = keywordEnricher;
        this.summaryEnricher = summaryEnricher;
    }

    /**
     * 원본 문서 리스트를 입력받아 RAG용으로 최적화된 문서 리스트를 반환
     */
    public List<Document> processDocuments(List<Document> rawDocs) {

        // [Step 1] 개인정보 마스킹 (보안)
        List<Document> maskedDocs = piiMaskingTransformer.transform(rawDocs);

        // [Step 2] 포맷 통일 (메타데이터 규칙 적용)
        var formatter = DefaultContentFormatter.builder()
                .withExcludedEmbedMetadataKeys("file_path", "date_created")
                .withExcludedInferenceMetadataKeys("internal_id")
                .build();
        var formatTransformer = new ContentFormatTransformer(formatter);
        List<Document> formattedDocs = formatTransformer.transform(maskedDocs);

        // [Step 3] 청킹 (분할)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)   // 청크 크기
                .build();
        List<Document> chunks = splitter.split(formattedDocs);

        // [Step 4] 키워드 추출 (Metadata Enrichment 1)
        // 각 청크의 키워드를 뽑아 메타데이터에 추가
        List<Document> keywordEnriched = keywordEnricher.enrichWithCustomTemplate(chunks);

        // [Step 5] 문맥 요약 (Metadata Enrichment 2)
        // 각 청크의 앞뒤 문맥을 요약하여 연결성을 확보
        // 이 단계가 끝나면 RAG를 위한 고품질 데이터 준비 완료
        return summaryEnricher.enrichWithCustomTemplate(keywordEnriched);
    }
}
