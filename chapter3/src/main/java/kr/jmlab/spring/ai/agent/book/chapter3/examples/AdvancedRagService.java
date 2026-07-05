package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase;
import kr.jmlab.spring.ai.agent.book.chapter3.support.RagKnowledgeBase.IndexReport;
import reactor.core.publisher.Flux;

/**
 * RetrievalAugmentationAdvisor 기반 Advanced RAG 구현.
 */
@Service
public class AdvancedRagService {

    private static final double DISPLAY_SIMILARITY_THRESHOLD = 0.50;
    private static final int DISPLAY_TOP_K = 5;

    private final ChatClient chatClient;
    private final RagKnowledgeBase knowledgeBase;
    private final VectorStore vectorStore;

    public AdvancedRagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
            RagKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        this.vectorStore = vectorStore;

        // [Pre-Retrieval] 질문 재작성 모듈
        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .targetSearchSystem("Spring AI RAG vector store")
                .build();

        // [Retrieval] 문서 검색 모듈
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50) // 후보군 확보를 위해 관대하게 설정
                .topK(10)
                .build();

        // [Post-Retrieval] 문서 후처리 모듈
        DocumentPostProcessor activeDocumentFilter = (query, docs) -> docs.stream()
                .filter(doc -> "true".equals(String.valueOf(doc.getMetadata().get("isActive"))))
                .toList();

        // [Generation] 최종 RAG 프롬프트 생성 모듈
        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(new PromptTemplate("""
                        당신은 IT 전문 기술 지원 AI입니다.
                        아래의 [기술 문서]를 기반으로 사용자의 질문에 친절하게 답변해 주세요.
                        문서에 없는 내용은 지어내지 말고 솔직하게 모른다고 답해 주세요.
                        
                        [기술 문서]
                        {context}
                        
                        [질문]
                        {query}
                        
                        [답변]
                        """))
                .emptyContextPromptTemplate(new PromptTemplate("""
                        사용자의 질문에 대한 근거 문서를 찾지 못했습니다.
                        검색 가능한 문서 범위 안에서 다시 질문하도록 안내하세요.
                        """))
                .allowEmptyContext(false) // 검색 결과가 없으면 답변을 거부하도록 설정(안전장치)
                .build();

        // [Advisor 조립] 전처리 -> 검색 -> 후처리 -> 생성 순서로 파이프라인을 구축
        Advisor advancedRagAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)      // 1. 전처리(변환)
                .documentRetriever(documentRetriever)     // 2. 검색
                .documentPostProcessors(activeDocumentFilter, new KeywordFilteringPostProcessor())   // 3. 후처리(필터링)
                .queryAugmenter(queryAugmenter)           // 4. 생성(프롬프트 결합)
                .build();

        // ChatClient에 Advisor 등록
        this.chatClient = chatClientBuilder.clone()
                .defaultAdvisors(advancedRagAdvisor)
                .build();
    }

    public String ask(String question) {
        knowledgeBase.indexIfNecessary();
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    public Flux<String> stream(String question) {
        knowledgeBase.indexIfNecessary();
        return chatClient.prompt()
                .user(question)
                .stream()
                .content();
    }

    public IndexReport prepareKnowledgeBase() {
        return knowledgeBase.indexOffline();
    }

    public RagPipeline pipeline() {
        return new RagPipeline(
                "RewriteQueryTransformer(targetSearchSystem=Spring AI RAG vector store)",
                "VectorStoreDocumentRetriever(topK=10, similarityThreshold=0.50)",
                List.of(
                        "isActive 메타데이터 필터",
                        "KeywordFilteringPostProcessor(질문에 '긴급'이 포함되면 긴급 문서만 유지)"
                ),
                "ContextualQueryAugmenter(allowEmptyContext=false, Korean prompt templates)",
                "RetrievalAugmentationAdvisor");
    }

    public List<Document> retrieve(String question) {
        knowledgeBase.indexIfNecessary();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(DISPLAY_TOP_K)
                .similarityThreshold(DISPLAY_SIMILARITY_THRESHOLD)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request).stream()
                .filter(document -> "true".equals(String.valueOf(document.getMetadata().get("isActive"))))
                .toList();

        if (question.toLowerCase(Locale.ROOT).contains("긴급")) {
            return documents.stream()
                    .filter(document -> document.getText().contains("긴급"))
                    .toList();
        }

        return documents;
    }

    public record RagPipeline(
            String queryTransformer,
            String documentRetriever,
            List<String> documentPostProcessors,
            String queryAugmenter,
            String advisor) {
    }
}
