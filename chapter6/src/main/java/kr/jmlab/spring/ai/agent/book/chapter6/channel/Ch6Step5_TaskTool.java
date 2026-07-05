package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Qualifier;
import kr.jmlab.spring.ai.agent.book.chapter6.orchestration.SpringAIAgent;

/**
 * Step 5 (강화 에이전트): 하위 에이전트를 클라이언트 측 TaskTool로 위임한다.
 *
 * <p>Step 4의 하위 에이전트는 서버에 <b>손수</b> 만든 것이었다(MCP 서버 + ChatClient). 같은 "에이전트도 툴"을
 * 클라이언트 쪽에서는 커뮤니티의 TaskTool로 <b>선언적으로</b> 구성한다. {@code src/main/resources/agents}의
 * 마크다운 정의(report-writer)만 두면, 메인 에이전트가 보고서 작성 같은 독립 작업을 격리된 컨텍스트의
 * 하위 에이전트에 위임하고 결과 텍스트만 받는다(서버에 손수 만드는 방식과 선언적 위임 방식을 나란히 비교).</p>
 *
 * <p>클라이언트 측 커뮤니티 기능이라 MCP 서버가 필요 없다. 단독으로 실행한다:
 * {@code --spring.ai.cli.step=ch6-step5 --spring.ai.mcp.client.enabled=false}</p>
 */
@Component
@Profile("!ops & !knowledge")
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch6-step5")
class Ch6Step5_TaskTool implements CommandLineRunner {

    private final SpringAIAgent agent;

    Ch6Step5_TaskTool(@Qualifier("enhancedAgent") SpringAIAgent agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch6] Step5: 하위 에이전트 위임 (TaskTool, 멀티 경로 b) ===");
        String conversationId = UUID.randomUUID().toString();
        String question = """
                다음 내용으로 업무 보고서 초안을 report-writer 하위 에이전트에 맡겨 작성해줘.
                - 강남점 SKU-300(RAG 실습 데이터셋) 재고가 4개로 소진 임박
                - 최근 4주 주간 평균 판매 6개
                - 재발주 검토 필요""";
        System.out.println("> " + question);
        System.out.print("AI: ");
        agent.run(question, conversationId)
                .doOnNext(System.out::print)
                .blockLast();
        System.out.println();
    }
}
