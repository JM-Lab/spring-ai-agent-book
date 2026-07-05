package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 데이터베이스 게시글 읽기 커스텀 구현.
 */
@Component
@ConditionalOnBean(JdbcTemplate.class) // JdbcTemplate 빈이 있을 때만 등록 (DataSource 없는 환경 보호)
public class CustomDatabaseReader implements DocumentReader {

    private final JdbcTemplate jdbcTemplate;

    public CustomDatabaseReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Document> get() {
        // 1. 데이터베이스에서 게시글 조회
        // 실제 운영 환경에서는 대용량 처리를 위해 페이징이나 커서 방식이 필요할 수 있음
        String sql = "SELECT id, title, content, created_at, category FROM articles WHERE published = true";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            // 2. 데이터 추출
            String id = rs.getString("id");
            String title = rs.getString("title");
            String content = rs.getString("content");

            // 3. 메타데이터 구성
            // 메타데이터는 추후 검색 필터링(예: 특정 기간, 특정 카테고리만 검색)에 핵심적으로 사용됨
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("articleId", id);
            metadata.put("title", title);
            metadata.put("createdAt", rs.getString("created_at"));
            metadata.put("category", rs.getString("category"));
            metadata.put("sourceType", "database");

            // 4. Document 객체 생성
            // 본문(content)과 메타데이터(metadata)를 결합하여 반환
            return new Document(content, metadata);
        });
    }
}
