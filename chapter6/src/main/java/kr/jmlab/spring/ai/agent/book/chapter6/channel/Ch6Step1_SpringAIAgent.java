package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 1 (코어): 스프링 AI 코어만으로 조립한 메인 에이전트, 단독 실행.
 *
 * <p>MCP 서버 없이 클라이언트 코어만 띄운다. {@link SpringAIAgent}는 주입받은 ChatClient를 실행만 하고,
 * 그 코어 ChatClient(로컬 @Tool + ToolCallingAdvisor 툴 호출 루프 + MessageChatMemoryAdvisor 메모리)는
 * {@code AgentConfig.coreAgent}가 조립한다. 루프 코드 한 줄 없이도 에이전트가 시간 조회, 재고 조회, 예약 툴을
 * 스스로 호출하고, 직전 대화를 기억한다.</p>
 *
 * <p>실행(MCP 서버가 없으므로 MCP 클라이언트를 꺼서 단독 부팅):
 * {@code --spring.ai.cli.step=ch6-step1 --spring.ai.mcp.client.enabled=false}</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step1")
class Ch6Step1_SpringAIAgent implements CommandLineRunner {

    private final SpringAIAgent agent;

    Ch6Step1_SpringAIAgent(@Qualifier("coreAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step1: 코어 메인 에이전트 (로컬 툴만, 단독 실행) ===");
        String conversationId = UUID.randomUUID().toString();
        ask("지금 한국 시간을 알려주고, SKU-200 재고를 확인한 뒤 2개 예약해줘", conversationId);
        ask("방금 예약한 상품 이름이 뭐였지?", conversationId);   // 메모리 확인
    }

    private void ask(String question, String conversationId) {
        System.out.println("\n> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
