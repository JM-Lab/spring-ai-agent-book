package kr.jmlab.spring.ai.agent.book.chapter3;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 3장 실습용 로컬 벡터 저장소 구성.
 *
 * <p>운영 환경에서는 PgVector, Redis, Elasticsearch 같은 외부 벡터 저장소로
 * 교체할 수 있지만, 3장 학습 프로젝트는 인프라 없이 실행하기 위해
 * SimpleVectorStore를 사용한다.</p>
 */
@Configuration
class Chapter3RagConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
