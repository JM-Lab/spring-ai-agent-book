package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.FileEtlPipeline;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.LoggingDocumentWriter;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase;

/**
 * 3장 Step 3: DocumentWriter 로 ETL 파이프라인의 Load 단계를 확인한다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step3")
class Ch3Step3_DocumentWriters implements CommandLineRunner {

    private final Resource policyDocs;
    private final RagKnowledgeBase knowledgeBase;
    private final FileEtlPipeline fileEtlPipeline;
    private final LoggingDocumentWriter loggingDocumentWriter;

    Ch3Step3_DocumentWriters(
            @Value("classpath:data/policy-docs.txt") Resource policyDocs,
            RagKnowledgeBase knowledgeBase,
            FileEtlPipeline fileEtlPipeline,
            LoggingDocumentWriter loggingDocumentWriter) {
        this.policyDocs = policyDocs;
        this.knowledgeBase = knowledgeBase;
        this.fileEtlPipeline = fileEtlPipeline;
        this.loggingDocumentWriter = loggingDocumentWriter;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step3: DocumentWriter / Load ===");

        List<Document> preparedDocuments = knowledgeBase.loadDocuments();
        loggingDocumentWriter.write(preparedDocuments);
        System.out.printf("LoggingDocumentWriter 처리 문서 수: %d%n", preparedDocuments.size());

        FileEtlPipeline.EtlResult result = fileEtlPipeline.runPipeline(policyDocs);
        System.out.printf("FileDocumentWriter 출력 파일: %s%n", result.outputPath().toAbsolutePath());
        System.out.printf("FileDocumentWriter 저장 청크 수: %d%n", result.documentCount());

        System.out.println("\nVectorStore 역시 DocumentWriter를 상속하므로, 같은 Document 목록을 add/write 단계의 입력으로 사용할 수 있습니다.");
    }
}
