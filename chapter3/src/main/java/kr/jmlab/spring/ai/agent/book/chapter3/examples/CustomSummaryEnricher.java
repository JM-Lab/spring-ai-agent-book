package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.stereotype.Component;

/**
 * SummaryMetadataEnricher 커스텀 템플릿 사용.
 */
@Component
public class CustomSummaryEnricher {

    private final ChatModel chatModel;

    public CustomSummaryEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public List<Document> enrichWithCustomTemplate(List<Document> docs) {

        /*
         * [참고] 스프링 AI의 SummaryMetadataEnricher 기본 템플릿(Default) 형태:
         * "Here is the content of the section: {context_str}. Summarize the key topics..."
         *
         * 위 기본 템플릿 대신, 아래처럼 내가 원하는 조건(기술 문서 특화 등)을
         * 구체적으로 명시한 커스텀 템플릿을 정의
         */
        String technicalTemplate = """
                다음은 기술 문서의 일부입니다. 이 섹션의 내용을 기술적 관점에서 요약해 주세요.

                [지침]
                1. 이 섹션에서 설명하는 '기술 주제'나 '기능'이 무엇인지 정의하세요.
                2. 동작 원리나 주요 특징을 포함하여 전체적인 흐름을 설명하세요.
                3. 불필요한 미사여구 없이 간결하고 명확한 기술 용어를 사용하세요.

                [텍스트]
                {context_str}

                [요약]
                """;

        // [핵심] 확장 생성자를 사용하여 커스텀 템플릿과 메타데이터 모드를 설정
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(
                chatModel,
                List.of(
                        SummaryMetadataEnricher.SummaryType.PREVIOUS,
                        SummaryMetadataEnricher.SummaryType.CURRENT,
                        SummaryMetadataEnricher.SummaryType.NEXT
                ),
                technicalTemplate, // 커스텀 템플릿 적용
                MetadataMode.ALL   // 요약 생성 시 문서의 모든 메타데이터를 참고하도록 설정
        );

        return enricher.transform(docs);
    }
}
