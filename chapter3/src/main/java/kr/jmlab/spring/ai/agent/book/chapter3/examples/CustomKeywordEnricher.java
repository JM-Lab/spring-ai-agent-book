package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

/**
 * KeywordMetadataEnricher 커스텀 템플릿 사용.
 */
@Component
public class CustomKeywordEnricher {

    private final ChatModel chatModel;

    public CustomKeywordEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public List<Document> enrichWithCustomTemplate(List<Document> docs) {

        /*
         * [참고] 스프링 AI의 KeywordMetadataEnricher 기본 템플릿(Default) 형태:
         * "{context_str}. Give %s unique keywords for this document..."
         *
         * keywordCount(5)를 설정하면, %s 자리에 5가 자동으로 삽입
         * 위 기본 템플릿 대신, 아래처럼 내가 원하는 조건(해시태그 등)을
         * 명시한 커스텀 템플릿을 정의
         */
        PromptTemplate hashtagTemplate = new PromptTemplate(
                """
                다음 텍스트를 분석하여 SNS에 공유하기 좋은
                트렌디한 해시태그를 5개 생성해 주세요.

                [제약 사항]
                1. 각 키워드 앞에 '#'을 붙일 것
                2. 쉼표(,)로만 구분할 것

                텍스트: {context_str}

                해시태그:
                """
        );

        /*
         * [핵심]
         * 1. 위 템플릿의 {context_str} 부분에 Document 객체의 텍스트가
         *  자동으로 채워져서 LLM에게 전송
         * 2. keywordsTemplate을 설정하면 .keywordCount() 설정은 무시
         */
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
                .keywordsTemplate(hashtagTemplate)
                .build();

        /*
         * [결과 비교]
         * 기본 템플릿: "Spring AI, Java, Framework"
         * 커스텀 템플릿: "#SpringAI, #JavaDev, #TechTrend"
         */

        return enricher.transform(docs);
    }
}
