package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

/**
 * 개인정보 마스킹 DocumentTransformer.
 */
@Component
public class PiiMaskingTransformer implements DocumentTransformer {

    // 이메일 패턴 (예시)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    // 전화번호 패턴 (한국 형식: 010-1234-5678)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("01[0-9]-\\d{3,4}-\\d{4}");

    @Override
    public List<Document> apply(List<Document> documents) {
        return documents.stream()
                .map(this::maskDocument)
                .collect(Collectors.toList());
    }

    private Document maskDocument(Document doc) {
        String content = doc.getText();

        // 1. 이메일 마스킹 -> [MASKED_EMAIL]
        if (EMAIL_PATTERN.matcher(content).find()) {
            content = EMAIL_PATTERN.matcher(content).replaceAll("[MASKED_EMAIL]");
        }

        // 2. 전화번호 마스킹 -> [MASKED_PHONE]
        if (PHONE_PATTERN.matcher(content).find()) {
            content = PHONE_PATTERN.matcher(content).replaceAll("[MASKED_PHONE]");
        }

        // 원본 메타데이터는 유지하고, 마스킹된 내용으로 새 문서 생성
        return new Document(content, doc.getMetadata());
    }
}
