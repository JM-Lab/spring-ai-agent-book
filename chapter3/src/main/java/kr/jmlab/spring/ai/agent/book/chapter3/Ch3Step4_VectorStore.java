package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.SearchRequestExamples;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.VectorStoreLifecycleService;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase;

/**
 * 3장 Step 4: 임베딩 모델과 SimpleVectorStore 로 유사도 검색 실행.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step4")
class Ch3Step4_VectorStore implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final RagKnowledgeBase knowledgeBase;

    Ch3Step4_VectorStore(VectorStore vectorStore, RagKnowledgeBase knowledgeBase) {
        this.vectorStore = vectorStore;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step4: Embedding + VectorStore similaritySearch ===");

        knowledgeBase.indexIfNecessary();

        String query = "Spring AI RAG에서 문서를 어떻게 검색하나요?";
        SearchRequestExamples.compare(query).forEach((name, request) -> {
            System.out.printf("%n[%s] query='%s', topK=%d, threshold=%.2f, filter=%s%n",
                    name,
                    request.getQuery(),
                    request.getTopK(),
                    request.getSimilarityThreshold(),
                    request.getFilterExpression());
            printResults(vectorStore.similaritySearch(request));
        });

        System.out.println("\n[문서 수명 주기] 구 버전을 삭제하고 새 버전을 적재하는 예시");
        VectorStoreLifecycleService lifecycleService = new VectorStoreLifecycleService(vectorStore);
        lifecycleService.addVersion("RAG-OPS-001", "1.0",
                "RAG 운영 문서 v1: 검색 결과가 없으면 운영자에게 문의합니다.");
        lifecycleService.replaceVersion("RAG-OPS-001", "1.0", "2.0",
                "RAG 운영 문서 v2: 검색 결과가 없으면 알 수 없다고 답하고, 문서 보강 요청을 기록합니다.");
        printResults(lifecycleService.searchActiveVersion("RAG-OPS-001", "검색 결과가 없을 때 어떻게 답변하나요?"));
    }

    private void printResults(List<Document> results) {
        results.forEach(doc -> {
            String text = doc.getText().replaceAll("\\s+", " ");
            System.out.printf("- score=%.4f, metadata=%s%n", doc.getScore(), doc.getMetadata());
            System.out.println("  " + text.substring(0, Math.min(text.length(), 160)));
        });
    }
}
