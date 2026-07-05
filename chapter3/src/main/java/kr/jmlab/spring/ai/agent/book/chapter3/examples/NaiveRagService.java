package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * QuestionAnswerAdvisor 를 사용한 Naive RAG 구현.
 */
@Service
public class NaiveRagService {

    private final ChatClient chatClient;

    public NaiveRagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {

        // 정적 필터 및 검색 파라미터 설정(Advisor 생성 시 고정됨)
        var searchRequest = SearchRequest.builder()
                // 유사도 임계값(0.0 ~ 1.0)
                .similarityThreshold(0.8)

                // Top-K 설정
                .topK(6)

                // 정적 메타데이터 필터링
                .filterExpression("category == 'tech_docs'")
                .build();

        // QuestionAnswerAdvisor 생성 및 ChatClient에 기본 어드바이저로 추가
        this.chatClient = chatClientBuilder
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(searchRequest) // 위에서 정의한 검색 조건 주입
                        .build())
                .build();
    }

    /**
     * 기본 질문 메서드(정적 설정 사용)
     * 생성자에서 설정한 기본 검색 조건(유사도 0.8, Top-6)을 그대로 사용
     */
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 동적 필터링을 적용한 질문 메서드(런타임 설정 사용)
     * 상황에 따라 검색 범위를 제한해야 할 때(예: 사용자별 권한, 특정 카테고리) 사용
     */
    public String askWithFilter(String question, String category) {
        return chatClient.prompt()
                .user(question)
                // 런타임에 동적으로 필터 표현식 주입(Advisor 컨텍스트 파라미터 활용)
                // Advisor 생성 시점의 정적 필터 설정을 덮어쓰는 방식으로 동작
                // 멀티 테넌트(Multi-tenant) 환경에서 데이터 격리를 구현할 때 필수
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
                        "category == '" + category + "'"))
                .call()
                .content();
    }
}
