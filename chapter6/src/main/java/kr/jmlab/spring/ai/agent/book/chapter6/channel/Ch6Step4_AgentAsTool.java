package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 4: 강화 에이전트로 전환하고 지식 서버로 "에이전트도 툴"을 실증한다.
 *
 * <p>강화 에이전트로 넘어가는 단계다. 코어 {@code SpringAIAgent}를 커뮤니티 능력까지 갖춘 강화 에이전트(enhancedAgent 빈)로
 * 전환하고, 운영 서버에 더해 지식(Knowledge) MCP 서버를 <b>하나 더</b> 연결한다. 서버를 더하기만 하면 능력이
 * 늘어난다(개방-폐쇄 원칙). 지식 서버의 {@code rag_answer_question} 툴은 단순 함수가 아니라 서버 안에서
 * 검색, 추론, 답변을 모두 수행하는 하위 에이전트다. 메인 에이전트가 보기에는 외부 툴 한 번 호출이지만,
 * 그 경계 너머에서는 또 하나의 에이전트 루프가 돈다. 멀티 에이전트의 첫 번째 경로(서버 측 agent-as-a-tool)다.</p>
 *
 * <p>실행 전 두 MCP 서버를 모두 띄우고, 클라이언트 설정의 {@code knowledge} 연결 주석을 해제한다
 * (MCP 클라이언트는 설정된 서버가 모두 떠 있어야 부팅된다).<br>
 * 터미널 1: {@code --spring.profiles.active=ops}, 터미널 2: {@code --spring.profiles.active=knowledge}<br>
 * 실행: {@code --spring.ai.cli.step=ch6-step4}</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step4")
class Ch6Step4_AgentAsTool implements CommandLineRunner {

    private final SpringAIAgent agent;

    Ch6Step4_AgentAsTool(@Qualifier("enhancedAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step4: 지식 서버 agent-as-a-tool ===");
        String conversationId = UUID.randomUUID().toString();
        String question = "사내 정책상 비밀번호는 얼마나 자주 변경해야 하는지 알려줘";
        System.out.println("> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
