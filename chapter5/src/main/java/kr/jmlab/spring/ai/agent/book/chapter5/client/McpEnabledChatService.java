package kr.jmlab.spring.ai.agent.book.chapter5.client;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * 5장 최종 프로젝트: MCP 서버가 제공한 ToolCallback을 ChatClient에 연결한다.
 */
@Service
@Profile("!server")
public class McpEnabledChatService {

    /**
     * 5장 프로젝트는 두 가지 RAG 아키텍처 패턴을 모두 보여 준다.
     * <ul>
     *   <li>클라이언트 측 답변(기본): rag_search_documents 로 문서만 가져오고, 클라이언트의 ChatClient 가 답변을 구성한다.</li>
     *   <li>서버 측 답변: rag_answer_question 으로 서버 안의 ChatClient 가 답변까지 생성해 반환한다.</li>
     * </ul>
     * 시스템 프롬프트는 기본 패턴인 검색 전용을 안내한다.
     * 서버 측 답변 패턴은 CLI 의 /answer-direct 명령으로 명시적으로 호출한다.
     */
    private static final String SYSTEM_PROMPT = """
            당신은 Spring AI MCP 학습을 돕는 AI 어시스턴트입니다.
            다음 원칙을 지킵니다:
            - 사내 정책, RAG 운영, Spring AI 문서 처리, 제품 카탈로그 질문은 MCP 서버의 RAG 툴을 사용합니다.
            - 기본 동작은 rag_search_documents 툴로 관련 문서를 검색하고, 검색 결과(sources)를 근거로 한국어 답변을 직접 구성합니다(클라이언트 측 답변 생성).
            - rag_answer_question 툴은 사용자가 명시적으로 "서버에서 답변" 또는 "서버 측 답변"을 요청할 때만 사용합니다.
            - 툴이 반환한 sources 를 확인하고, 문서에 없는 사실은 단정하지 않습니다.
            - 답변은 한국어로 간결하게 작성하고 필요한 경우 출처 파일명을 함께 언급합니다.
            """;

    private final ChatClient chatClient;

    public McpEnabledChatService(
            ChatClient.Builder builder,
            SyncMcpToolCallbackProvider toolCallbackProvider) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = builder.clone()
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                                .build(),
                        ToolCallingAdvisor.builder()
                                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300)
                                .build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    public Flux<String> stream(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .toolContext(Map.of(
                        "userId", "chapter5-cli-user",
                        "conversationId", conversationId,
                        "clientSession", "ch5-final"
                ))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
