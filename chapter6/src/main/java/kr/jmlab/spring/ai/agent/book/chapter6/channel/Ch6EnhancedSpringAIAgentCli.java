package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.Scanner;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * final: 통합 엔터프라이즈 스프링 AI 에이전트 CLI.
 *
 * <p>로컬 @Tool, 커뮤니티 능력(TodoWrite, AskUserQuestion, Skills, TaskTool),
 * 그리고 MCP 서버의 하위 에이전트를 모두 단일 툴 인터페이스로 갖춘 메인 에이전트와 대화한다.
 * 위험 작업 앞에서는 @McpElicitation 승인 게이트가, 모호한 요청에는 AskUserQuestion이 사람을 부른다.</p>
 *
 * <p>실행 전 다른 터미널에서 MCP 서버를 먼저 띄운다(운영, 지식 두 서버):
 * {@code --spring.profiles.active=ops} 와 {@code --spring.profiles.active=knowledge}.
 * 옵션 없이(기본 클라이언트 역할로) 실행하면 이 통합 CLI가 뜬다.</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-final", matchIfMissing = true)
class Ch6EnhancedSpringAIAgentCli implements CommandLineRunner {

    private final SpringAIAgent agent;
    private final String conversationId = UUID.randomUUID().toString();

    Ch6EnhancedSpringAIAgentCli(@Qualifier("enhancedAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== 엔터프라이즈 스프링 AI 에이전트 CLI ===");
        System.out.println("무엇이든 시켜 보세요. 종료는 /exit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isBlank()) {
                    continue;
                }
                if ("/exit".equalsIgnoreCase(input) || "/quit".equalsIgnoreCase(input)) {
                    System.out.println("종료합니다.");
                    break;
                }
                System.out.print("AI: ");
                try {
                    agent.run(input, conversationId)
                            .doOnNext(System.out::print)
                            .blockLast();
                } catch (RuntimeException ex) {
                    // 스트리밍 중 일시적 오류(일부 로컬 리즈닝 모델의 스트리밍 청크 처리 문제 등)가 나더라도
                    // 세션 전체를 끝내지 않고 이 턴만 건너뛴다. 재시도(retry)는 비가역 툴을 중복 실행할 수 있어 하지 않는다.
                    System.out.print("[이번 응답을 완성하지 못했습니다. 다시 시도해 주세요 ("
                            + ex.getClass().getSimpleName() + ")]");
                }
                System.out.println();
            }
        }
    }
}
