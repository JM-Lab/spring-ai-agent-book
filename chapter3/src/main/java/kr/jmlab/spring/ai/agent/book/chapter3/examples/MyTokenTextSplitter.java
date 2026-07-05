package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * TokenTextSplitter 생성 방법.
 */
@Component
public class MyTokenTextSplitter {

    /**
     * 기본 설정 사용
     * 별도의 설정 없이 기본값(청크 크기 800 토큰 등)으로 설정
     */
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        return splitter.split(documents);
    }

    /**
     * 빌더 패턴 사용
     * 프로젝트 요구사항에 맞춰 청크 크기, 최소 길이 등을 정밀하게 설정
     */
    public List<Document> splitWithBuilder(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(1000)           // 목표 청크 크기 (토큰)
                .withMinChunkSizeChars(400)    // 최소 문자 수 (너무 짧은 조각 방지)
                .withMinChunkLengthToEmbed(10) // 임베딩할 최소 길이 (의미 없는 단문 제외)
                .withMaxNumChunks(5000)        // 최대 청크 수 (안전장치)
                .withKeepSeparator(true)       // 문단 구분자(\n) 유지 여부
                .build();

        return splitter.split(documents);
    }

    /**
     * 다국어 커스텀 설정(Custom Punctuation Marks)
     * 문장을 의미 있게 나누기 위해 사용되는 문장 부호를 커스터마이징
     * 이는 국제화(i18n) 처리나 CJK(한중일) 텍스트가 섞인 경우에 특히 유용
     */
    public List<Document> splitMixedText(List<Document> documents) {
        // 영어 기본 부호와 다국어(CJK) 부호를 혼합하여 사용
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withPunctuationMarks(List.of(
                        '.', '?', '!', '\n', // [기본] 영어/한국어용
                        ';', ':',            // [추가] 세미콜론 등 추가 구두점
                        '。', '？', '！'     // [확장] 일본어/중국어 등 다국어 처리용
                ))
                .build();

        return splitter.split(documents);
    }
}
