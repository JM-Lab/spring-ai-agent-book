package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;

/**
 * 에이전트 루프 안전 가드.
 *
 * <p>{@code ToolCallingAdvisor}는 모델 응답에 툴 호출이 있으면 루프를 한 번 더 돈다. 정상적으로는
 * 모델이 더 이상 툴을 부르지 않을 때 멈추지만, 모델이 같은 툴을 끝없이 부르는 비정상 상황에서는
 * 루프가 멈추지 않을 수 있다. 실제 발주처럼 <b>비가역 작업</b>을 다루는 에이전트라면 이런 폭주를
 * 막는 상한이 반드시 필요하다.</p>
 *
 * <p>스프링 AI는 "이번 응답을 받고 툴 루프를 계속할지"를 판단하는 자리를
 * {@link ToolExecutionEligibilityChecker}로 열어 둔다. 이 체커를
 * {@code ToolCallingAdvisor.Builder.toolExecutionEligibilityChecker(...)}에 끼우면, 한 요청에서 도는
 * 툴 실행 라운드 수가 상한을 넘는 순간 루프를 강제로 종료한다.</p>
 */
public final class AgentSafety {

    private AgentSafety() {
    }

    /** 한 요청당 최대 maxRounds 번의 툴 실행 라운드만 허용. 호출될 때마다 새 카운터를 가진 새 체커를 돌려줌 */
    public static ToolExecutionEligibilityChecker maxToolRounds(int maxRounds) {
        AtomicInteger rounds = new AtomicInteger();        // 이 체커 하나(= 한 요청)의 라운드 수
        return (ChatResponse response) -> {
            if (response == null || !response.hasToolCalls()) {  // 모델이 툴을 더 안 부름 → 정상 종료
                return false;
            }
            return rounds.incrementAndGet() <= maxRounds;         // 상한 안이면 한 라운드 더, 넘으면 중단
        };
    }
}
