package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.CustomContentFormatExample;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.PiiMaskingTransformer;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase;

/**
 * 3장 Step 2: DocumentTransformer 로 문서 정제와 청킹 수행.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step2")
class Ch3Step2_DocumentTransformers implements CommandLineRunner {

    private final RagKnowledgeBase knowledgeBase;
    private final PiiMaskingTransformer piiMaskingTransformer;
    private final CustomContentFormatExample contentFormatExample;

    Ch3Step2_DocumentTransformers(
            RagKnowledgeBase knowledgeBase,
            PiiMaskingTransformer piiMaskingTransformer,
            CustomContentFormatExample contentFormatExample) {
        this.knowledgeBase = knowledgeBase;
        this.piiMaskingTransformer = piiMaskingTransformer;
        this.contentFormatExample = contentFormatExample;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step2: DocumentTransformer ===");

        List<Document> sourceDocs = knowledgeBase.loadRawDocuments();
        List<Document> maskedDocs = piiMaskingTransformer.transform(sourceDocs);
        List<Document> formattedDocs = contentFormatExample.formatDocuments(maskedDocs);

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(180)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(20)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.split(formattedDocs);
        System.out.println("원본 문서 수: " + sourceDocs.size());
        System.out.println("마스킹 후 문서 수: " + maskedDocs.size());
        System.out.println("청크 수: " + chunks.size());
        chunks.stream().limit(3).forEach(doc -> {
            String text = doc.getText().replaceAll("\\s+", " ");
            System.out.println("- " + text.substring(0, Math.min(text.length(), 140)));
            System.out.println("  metadata: " + doc.getMetadata());
        });
    }
}
