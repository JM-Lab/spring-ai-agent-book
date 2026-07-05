package kr.jmlab.spring.ai.agent.book.chapter5;

import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter5.client.McpClientCatalogService;
import kr.jmlab.spring.ai.agent.book.chapter5.client.McpEnabledChatService;

/**
 * 5장 최종 프로젝트: MCP 기반 AI Chatbot CLI.
 */
@Component
@Profile("!server")
@ConditionalOnProperty(
        prefix = "spring.ai.cli",
        name = "step",
        havingValue = "ch5-final",
        matchIfMissing = true)
class Ch5McpCliChatbotApplication implements CommandLineRunner {

    private final McpEnabledChatService chatService;
    private final McpClientCatalogService catalogService;
    private final String conversationId = UUID.randomUUID().toString();

    Ch5McpCliChatbotApplication(
            McpEnabledChatService chatService,
            McpClientCatalogService catalogService) {
        this.chatService = chatService;
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        printBanner();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if (input == null || input.isBlank()) {
                    continue;
                }
                String trimmed = input.trim();
                if ("/exit".equalsIgnoreCase(trimmed) || "/quit".equalsIgnoreCase(trimmed)) {
                    System.out.println("대화를 종료합니다.");
                    break;
                }

                if (handleCommand(trimmed)) {
                    continue;
                }

                System.out.print("AI: ");
                chatService.stream(input, conversationId)
                        .doOnNext(System.out::print)
                        .blockLast();
                System.out.println("\n");
            }
        }
    }

    private boolean handleCommand(String input) {
        if ("/tools".equalsIgnoreCase(input)) {
            System.out.println(catalogService.listTools());
            return true;
        }
        if ("/callbacks".equalsIgnoreCase(input)) {
            System.out.println(catalogService.listToolCallbacks());
            return true;
        }
        if ("/resources".equalsIgnoreCase(input)) {
            System.out.println(catalogService.listResources());
            return true;
        }
        if (input.startsWith("/resource ")) {
            System.out.println(catalogService.readResource(input.substring("/resource ".length()).trim()));
            return true;
        }
        if ("/prompts".equalsIgnoreCase(input)) {
            System.out.println(catalogService.listPrompts());
            return true;
        }
        if (input.startsWith("/prompt ")) {
            String question = input.substring("/prompt ".length()).trim();
            System.out.println(catalogService.getPrompt("rag-grounded-answer", Map.of(
                    "question", question,
                    "category", "tech_docs"
            )));
            return true;
        }
        if (input.startsWith("/complete ")) {
            System.out.println(catalogService.completeCategory(input.substring("/complete ".length()).trim()));
            return true;
        }
        if (input.startsWith("/answer-direct ")) {
            // 서버 측 답변 패턴: rag_answer_question 툴을 직접 호출해 서버 내부 LLM 이 생성한 답변을 받는다.
            String question = input.substring("/answer-direct ".length()).trim();
            if (question.isBlank()) {
                System.out.println("질문을 입력하세요. 예: /answer-direct RAG 운영 정책을 알려줘");
            } else {
                System.out.println(catalogService.answerDirect(question));
            }
            return true;
        }
        return false;
    }

    private void printBanner() {
        System.out.println("""

                ══════════════════════════════════════════════════════
                  Spring AI MCP CLI  (Chapter 5, Final)
                  MCP Client, MCP Server, ToolCallbackProvider
                  Chapter 3 RAG pipeline exposed as an MCP server
                  Model: qwen3.5:4b (via Ollama @ localhost:11434)
                  Server: http://localhost:8085/mcp
                  종료: /exit  또는  /quit
                ══════════════════════════════════════════════════════

                Conversation ID: %s
                기본 동작: 일반 채팅 입력은 클라이언트 측 답변 패턴 (rag_search_documents + 클라이언트 LLM)
                서버 측 답변 패턴: /answer-direct <question>  (rag_answer_question 직접 호출)
                기타 명령: /tools, /callbacks, /resources, /resource rag://sources,
                          /resource rag://pipeline, /prompts, /prompt <question>, /complete <category-prefix>
                """.formatted(conversationId));
    }
}
