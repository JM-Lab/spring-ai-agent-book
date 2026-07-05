package kr.jmlab.spring.ai.agent.book.chapter6.orchestration;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 모델의 사고(thinking)를 CLI에 보여 주는 어드바이저.
 *
 * <p>리즈닝 모델(qwen3 계열)은 답하기 전 "사고" 단계를 거친다. 스프링 AI의 OllamaChatModel은 그 사고를
 * 스트리밍 응답 <b>청크의 메타데이터 키 {@code "thinking"}</b>에 토큰 단위로 담는다. 다만 스트림을 한 라운드
 * 단위로 합치는 단계({@code MessageAggregator})에서 이 사고 메타데이터가 떨어져 나가, {@link BaseAdvisor}의
 * {@code after}(합쳐진 응답을 받는다)로는 볼 수 없다. 그래서 이 어드바이저는 {@code after}가 아니라
 * <b>원시 스트림을 직접 탭하는</b> {@link StreamAdvisor}로 구현해, 사고 토큰이 흐르는 대로 콘솔에 찍는다.</p>
 *
 * <p>한 라운드에서 모델은 먼저 사고하고(thinking 청크) 그 다음 툴을 부르거나 답을 낸다(content 청크). 그래서
 * 사고가 끝나는 순간 블록을 닫으면, "사고 → 툴 호출 → (다음 라운드) 사고 → 툴 호출 → 사고 → 답변"처럼
 * 에이전트가 생각하고 행동하기를 번갈아 하는 과정이 CLI에 그대로 드러난다.</p>
 */
public class ThinkTraceAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return "thinkTrace";
    }

    @Override
    public int getOrder() {
        return BaseAdvisor.HIGHEST_PRECEDENCE + 360; // 툴 호출 루프(+300) 안쪽
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(request); // 비스트리밍 호출에서는 사고를 따로 찍지 않는다
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        boolean[] open = {false};   // 이 라운드에서 [생각] 블록이 열려 있는지
        return chain.nextStream(request)
                .doOnNext(resp -> {
                    ChatResponse chatResponse = resp.chatResponse();
                    if (chatResponse == null || chatResponse.getResult() == null) {
                        return;
                    }
                    Object thinking = chatResponse.getResult().getOutput().getMetadata().get("thinking");
                    String token = thinking == null ? "" : thinking.toString();
                    if (!token.isEmpty()) {
                        if (!open[0]) {            // 사고 첫 토큰 → 블록 머리말
                            System.out.print("\n  [생각] ");
                            open[0] = true;
                        }
                        System.out.print(token);   // 사고를 흐르는 대로 출력
                    }
                    else if (open[0]) {            // 사고가 끝나고 툴 호출, 답변으로 넘어감 → 블록 닫기
                        System.out.println();
                        open[0] = false;
                    }
                })
                .doOnComplete(() -> {
                    if (open[0]) {
                        System.out.println();
                    }
                });
    }
}
