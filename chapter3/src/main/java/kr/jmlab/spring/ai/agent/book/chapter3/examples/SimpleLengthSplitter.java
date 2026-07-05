package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * [커스텀] 단순 문자 길이 기반 분할기
 * DocumentTransformer 를 상속한 TextSplitter 추상 클래스를 상속하여 구현
 */
public class SimpleLengthSplitter extends TextSplitter {

    private final int chunkLength;

    public SimpleLengthSplitter(int chunkLength) {
        this.chunkLength = chunkLength;
    }

    @Override
    protected List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();

        // 지정된 길이만큼 반복하며 텍스트를 분할
        for (int i = 0; i < length; i += chunkLength) {
            int end = Math.min(length, i + chunkLength);
            chunks.add(text.substring(i, end));
        }

        return chunks;
    }
}
