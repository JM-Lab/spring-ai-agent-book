package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 툴 호출과 그 결과를 CLI에 보여 주는 어드바이저.
 *
 * <p>{@link BaseAdvisor}라 순서값을 ToolCallingAdvisor(기본 {@code HIGHEST_PRECEDENCE + 300})보다 큰
 * {@code +350}으로 두어 <b>툴 호출 루프 안쪽</b>에 놓인다. 그래서 루프가 한 바퀴 돌 때마다 호출된다.</p>
 *
 * <ul>
 *   <li>{@link #after}: 그 라운드의 모델 응답에 담긴 <b>툴 호출</b>(이름, 인자)을 찍는다.</li>
 *   <li>{@link #before}: 직전 라운드의 <b>툴 결과</b>를 찍는다. 결과는 다음 라운드 요청에 {@code ToolResponseMessage}로
 *       담겨 오므로, 그 요청에서 가장 최근 결과를 꺼내 보여 준다.</li>
 * </ul>
 *
 * <p>그래서 한 요청을 처리하는 동안 "툴 호출 → (다음 라운드) 툴 결과 → 다음 툴 호출 → 결과"가 차례로 드러난다.</p>
 */
public class ToolCallTraceAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        List<Message> messages = request.prompt().getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {   // 가장 최근 ToolResponseMessage만
            if (messages.get(i) instanceof ToolResponseMessage toolResponses) {
                toolResponses.getResponses().forEach(response ->
                        System.out.printf("  [툴 결과] %s → %s%n", response.name(), response.responseData()));
                break;
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return response;
        }
        chatResponse.getResult().getOutput().getToolCalls()
                .forEach(toolCall -> System.out.printf("%n  [툴 호출] %s %s%n",
                        toolCall.name(), toolCall.arguments()));
        return response;
    }

    @Override
    public int getOrder() {
        return BaseAdvisor.HIGHEST_PRECEDENCE + 350; // ToolCallingAdvisor(+300) 안쪽 = 매 라운드 실행
    }
}
