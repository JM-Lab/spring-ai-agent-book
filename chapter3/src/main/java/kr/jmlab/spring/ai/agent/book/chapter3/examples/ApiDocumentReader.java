package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 외부 REST API 데이터 연동 커스텀 구현.
 */
@Component
@ConditionalOnBean(RestClient.Builder.class) // RestClient.Builder 빈이 있을 때만 등록
public class ApiDocumentReader implements DocumentReader {

    private final RestClient restClient;
    private final String apiUrl = "https://api.example.com/v1/notices";

    public ApiDocumentReader(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<Document> get() {
        List<Document> documents = new ArrayList<>();

        try {
            // 1. API 호출 및 응답 파싱
            NoticeResponse response = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(NoticeResponse.class);

            if (response != null && response.data() != null) {
                for (NoticeData data : response.data()) {
                    // 2. 본문 텍스트 구성
                    // 제목과 내용을 합쳐서 하나의 본문으로 만들면 검색 정확도 향상
                    String content = String.format("제목: %s\n내용: %s", data.title(), data.content());

                    // 3. 메타데이터 생성
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("source", "external_api");
                    metadata.put("noticeId", data.id());
                    metadata.put("publishedAt", data.publishedDate());

                    // 4. Document 추가
                    documents.add(new Document(content, metadata));
                }
            }
        } catch (Exception e) {
            // 실제 운영 시에는 로깅을 남기고 빈 리스트를 반환하거나 커스텀 예외를 던지는 처리 필요
            throw new RuntimeException("API 데이터 수집 중 오류 발생: " + apiUrl, e);
        }

        return documents;
    }

    // 내부 DTO 클래스 (API 응답 구조에 맞게 정의)
    record NoticeResponse(List<NoticeData> data) {}
    record NoticeData(String id, String title, String content, String publishedDate) {}
}
