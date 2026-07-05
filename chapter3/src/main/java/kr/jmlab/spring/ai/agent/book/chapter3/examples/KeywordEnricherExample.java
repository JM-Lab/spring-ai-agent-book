package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

/**
 * KeywordMetadataEnricher 기본 사용.
 */
@Component
public class KeywordEnricherExample {

    private final ChatModel chatModel;

    // ChatModel은 스프링 컨텍스트에서 자동으로 주입
    public KeywordEnricherExample(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public List<Document> enrich(List<Document> docs) {
        // [중요] Builder 시작 시 ChatModel을 필수로 전달 필요
        // 이 ChatModel이 실제로 키워드를 추출하는 LLM 호출을 담당
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
                .keywordCount(5) // 문서당 5개의 키워드 추출
                .build();

        // LLM 호출을 통해 키워드가 추가된 문서 반환
        // (문서 개수만큼 LLM 호출이 발생하므로 비용에 주의)
        return enricher.transform(docs);
    }
}
