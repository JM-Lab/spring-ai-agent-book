package kr.jmlab.spring.ai.agent.book.chapter5;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.support.RagDocumentLoader.IndexReport;
import kr.jmlab.spring.ai.agent.book.chapter5.support.RagMcpKnowledgeBase;

/**
 * 5장 Server Step 1: RAG 문서 ETL과 벡터 인덱싱 준비.
 */
@Component
@Profile("server")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch5-server-step1")
class Ch5ServerStep1_RagKnowledgeBase implements CommandLineRunner {

    private final RagMcpKnowledgeBase knowledgeBase;

    Ch5ServerStep1_RagKnowledgeBase(RagMcpKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch5] Server Step1: RAG 문서 준비와 벡터 인덱싱 ===");
        IndexReport report = knowledgeBase.getIndexReport();

        System.out.printf("documentsDirectory: %s%n", report.documentsDirectory());
        System.out.printf("rawDocumentCount: %d%n", report.rawDocumentCount());
        System.out.printf("maskedDocumentCount: %d%n", report.maskedDocumentCount());
        System.out.printf("chunkCount: %d%n", report.chunkCount());

        System.out.println("\n[Source Documents]");
        report.sourceDocuments().forEach(source -> System.out.printf(
                "- %s (%s): %d documents%n",
                source.filename(),
                source.sourceType(),
                source.documentCount()));

        System.out.println("\n[Pipeline]");
        System.out.println(knowledgeBase.pipelineDescription());
    }
}
