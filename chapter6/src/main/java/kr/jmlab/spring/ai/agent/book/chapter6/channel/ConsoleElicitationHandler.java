package kr.jmlab.spring.ai.agent.book.chapter6.channel;

import java.util.Map;
import java.util.Scanner;

import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;

import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Step 3: 사용자 개입 워크플로우(휴먼-인-더-루프) 승인 게이트(HITL, MCP 기반).
 *
 * <p>MCP 서버의 위험 툴이 실행 도중 {@code createElicitation}으로 승인을 요청하면,
 * 그 요청이 MCP 경계를 거슬러 올라와 이 핸들러(메인 에이전트 측)에 도착한다. 여기서 진짜 사람에게 묻고,
 * 거절하면 DECLINE을 돌려보내 서버 툴이 비가역 작업을 실행하지 않게 막는다.</p>
 *
 * <p>이 컴포넌트 하나로, 에이전트에 연결된 모든 MCP 서버의 위험 작업이 같은 승인 게이트를 지나간다.
 * 분산된 툴 생태계 전체에 일관된 휴먼-인-더-루프가 생긴다.</p>
 */
@Component
@Profile("!ops & !knowledge")
public class ConsoleElicitationHandler {

    private final Scanner scanner = new Scanner(System.in);   // 콘솔 입력 공유(매 호출마다 새로 만들면 입력이 유실됨)

    @McpElicitation(clients = "operations") // 운영 서버 연결 이름
    public ElicitResult onElicitation(ElicitRequest request) {
        // 서버에서 전달받은 메시지를 콘솔에 출력하고 사용자의 응답을 대기
        System.out.println("[승인 요청] " + request.message() + " (y/n)");
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "n";   // 입력이 없으면 안전하게 거절

        // 사용자가 'y'를 입력하지 않으면 즉시 거절 처리
        if (!"y".equalsIgnoreCase(answer)) {
            // 거절 시 서버 툴이 나머지 작업을 즉각 중단하도록 지시
            return new ElicitResult(ElicitResult.Action.DECLINE, null);
        }
        // 정상 승인 시 5장 분석 툴이 요구한 confirmed 스키마 값과 함께 승인 지시
        return new ElicitResult(ElicitResult.Action.ACCEPT, Map.of("confirmed", true));
    }
}
