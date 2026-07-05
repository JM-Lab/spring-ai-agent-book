package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

// 1. 커스텀 후처리기 구현(DocumentPostProcessor 인터페이스 구현)
public class KeywordFilteringPostProcessor implements DocumentPostProcessor {

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        // 사용자의 질문(Query) 텍스트 가져오기
        String queryText = query.text().toLowerCase();

        // 예시: 질문에 "긴급"이라는 단어가 있으면, 문서에도 "긴급"이 있는 것만 남김
        if (queryText.contains("긴급")) {
            return documents.stream()
                    .filter(doc -> doc.getText().contains("긴급"))
                    .toList();
        }

        // 별도 조건이 없으면 모든 문서 그대로 반환
        return documents;
    }
}
