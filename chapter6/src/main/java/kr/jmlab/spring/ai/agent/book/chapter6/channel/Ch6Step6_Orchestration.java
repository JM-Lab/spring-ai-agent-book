package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 6 (강화 에이전트): 오케스트레이션 확장: 작업 계획, 명확화 질문, 툴을 엮는 스킬.
 *
 * <p>메인 에이전트의 지휘 능력을 넓힌다. 모두 커뮤니티 라이브러리가 제공하는 능력이며, 강화 에이전트(enhancedAgent 빈)
 * 안에서 다른 툴과 똑같이 등록되어 있다.</p>
 *
 * <ul>
 *   <li><b>TodoWrite</b>: 여러 단계가 필요한 작업의 할 일 목록을 만들고 단계별로 진행한다(콘솔에 진행 상황 출력).</li>
 *   <li><b>AskUserQuestion</b>: 요청이 모호하면 가정하지 않고 사람에게 되묻는다(HITL, 로컬, MCP 없이).</li>
 *   <li><b>SkillsTool</b>: SKILL.md의 절차가 에이전트의 기존 툴(운영 MCP의 {@code check_stock}, {@code place_purchase_order})을 엮어 쓰게 한다.</li>
 * </ul>
 *
 * <p>이 데모는 사내 재발주 절차 스킬({@code restock-policy})을 시연한다. "재발주 검토" 요청에 에이전트가 스킬을
 * 불러와, 그 절차대로 재고를 조회({@code check_stock})하고 부족분을 계산한 뒤 필요하면 발주
 * ({@code place_purchase_order})까지 진행한다. <b>스킬 자체는 툴을 실행하지 않고</b> "무엇을 어떤 순서로
 * 호출하라"는 절차만 주입하며, 실제 호출은 에이전트의 툴 루프가 한다. 여러 단계라 작업 계획(TodoWrite)도 함께 나타날 수 있다.</p>
 *
 * <p>운영 서버의 툴을 엮으므로 MCP 서버(운영 서버)가 필요하다. 운영 서버를 띄운 뒤 실행한다.<br>
 * 실행: {@code --spring.ai.cli.step=ch6-step6} (상품을 묻는 질문에 답하고, 발주 승인 프롬프트에 y 또는 n 입력)</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step6")
class Ch6Step6_Orchestration implements CommandLineRunner {

    private final SpringAIAgent agent;

    Ch6Step6_Orchestration(@Qualifier("enhancedAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step6: 오케스트레이션, 툴을 엮는 스킬(SkillsTool) ===");
        String conversationId = UUID.randomUUID().toString();
        String question = "SKU-100, SKU-200, SKU-300 중 하나만 재고를 점검하고, 사내 재발주 정책에 따라 필요하면 발주까지 진행해줘";
        System.out.println("> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
