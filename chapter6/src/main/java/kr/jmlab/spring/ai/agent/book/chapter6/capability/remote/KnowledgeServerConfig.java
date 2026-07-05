package kr.jmlab.spring.ai.agent.book.chapter6.capability.remote;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RAG MCP 서버가 사용할 로컬 벡터 저장소 구성.
 */
@Configuration
@Profile("knowledge")
public class KnowledgeServerConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
