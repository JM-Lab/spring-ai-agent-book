package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 벡터 저장소 문서 수명 주기 관리 예제.
 */
public class VectorStoreLifecycleService {

    private final VectorStore vectorStore;

    public VectorStoreLifecycleService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addVersion(String docId, String version, String text) {
        Document document = new Document(text, Map.of(
                "docId", docId,
                "version", version,
                "isActive", true,
                "lastUpdated", LocalDate.now().toString()
        ));
        vectorStore.add(List.of(document));
    }

    public void replaceVersion(String docId, String oldVersion, String newVersion, String newText) {
        vectorStore.delete("docId == '" + docId + "' && version == '" + oldVersion + "'");
        addVersion(docId, newVersion, newText);
    }

    public List<Document> searchActiveVersion(String docId, String query) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThresholdAll()
                .filterExpression("docId == '" + docId + "' && isActive == true")
                .build());
    }
}
