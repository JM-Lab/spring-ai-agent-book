package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 다양한 JSON 처리 기법.
 */
@Component
public class JsonDocReader {

    private final Resource resource;

    public JsonDocReader(@Value("classpath:data/bikes.json") Resource resource) {
        this.resource = resource;
    }

    /**
     * 기본 필드 추출
     * - "description" 필드 값을 추출하여 문서 내용으로 사용
     */
    public List<Document> readJsonBasic() {
        JsonReader reader = new JsonReader(this.resource, "description");
        return reader.read();
    }

    /**
     * JSON Pointer를 활용한 중첩 데이터 추출
     * - bikes.json의 복잡한 구조에서 /store/bikes 배열만 콕 집어 추출
     * - 불필요한 store 메타 정보(name 등)를 제외하고 자전거 데이터만 문서화
     */
    public List<Document> readBikesWithPointer() {
        JsonReader reader = new JsonReader(this.resource, "description");
        // RFC 6901 표준 JSON Pointer 사용: /store/bikes 경로 지정
        return reader.get("/store/bikes");
    }

    /**
     * 특정 자전거 한 대만 추출
     * - JSON Pointer로 배열 인덱스 접근: /store/bikes/0 (첫 번째 자전거)
     */
    public List<Document> readFirstBikeOnly() {
        JsonReader reader = new JsonReader(this.resource, "description", "model");
        return reader.get("/store/bikes/0");
    }

    /**
     * 메타데이터 생성기를 활용한 고급 처리
     * - 문서 내용뿐만 아니라 메타데이터(브랜드, 가격 등)도 함께 추출
     */
    public List<Document> readBikesWithMetadata() {
        // 커스텀 메타데이터 생성기 정의
        JsonMetadataGenerator metadataGenerator = (jsonMap) -> {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("bikeBrand", jsonMap.get("brand"));
            metadata.put("bikeModel", jsonMap.get("model"));
            metadata.put("bikePrice", jsonMap.get("price"));
            metadata.put("sourceType", "productCatalog");
            return metadata;
        };

        // "description"은 문서의 본문 내용이 되고, brand, model, price는 메타데이터
        JsonReader reader = new JsonReader(this.resource, metadataGenerator, "description");
        return reader.get("/store/bikes");
    }
}
