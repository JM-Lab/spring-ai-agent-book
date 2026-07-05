package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 3: 비가역 작업의 승인 게이트(HITL, MCP 기반).
 *
 * <p>운영 서버 연결의 마지막 단계다. 운영 서버의 {@code place_purchase_order}(실제 발주)는 비가역 작업이라, 실행
 * 직전 서버가 {@code createElicitation}으로 승인을 요청한다. 이 요청은 MCP 경계를 거슬러 올라와
 * 클라이언트의 {@code ConsoleElicitationHandler}(@McpElicitation)에 도착하고, 콘솔에서 사람이 y/n로 답한다.
 * 즉 이 단계는 <b>서버 측(요청 발신)과 클라이언트 측(승인 처리)을 함께 수정</b>한다. 거절하면 발주는 실행되지
 * 않는다. 휴먼-인-더-루프는 모두 스프링 AI 코어(MCP Elicitation)만으로 동작한다.</p>
 *
 * <p>아울러 {@link SpringAIAgent}의 ToolCallingAdvisor에는 최대 반복 제한 안전 가드가 걸려 있어
 * ({@code AgentSafety.maxToolRounds}), 발주 같은 비가역 작업을 다루는 루프가 폭주하지 않는다.</p>
 *
 * <p>아직 커뮤니티 능력은 쓰지 않으므로 메인 에이전트는 여전히 {@link SpringAIAgent}(스프링 AI 코어만)다.
 * 실행 전 운영 서버를 띄운다(지식 서버는 아직 불필요).<br>
 * 실행: {@code --spring.ai.cli.step=ch6-step3} (승인 프롬프트에 y 또는 n 입력)</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step3")
class Ch6Step3_ApprovalGate implements CommandLineRunner {

    private final SpringAIAgent agent;

    Ch6Step3_ApprovalGate(@Qualifier("coreAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step3: 비가역 작업 승인 게이트 (HITL, MCP 기반) ===");
        String conversationId = UUID.randomUUID().toString();
        String question = "SKU-300을 10개 발주해줘";
        System.out.println("> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
