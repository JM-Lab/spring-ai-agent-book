package kr.jmlab.spring.ai.agent.book.chapter2.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

import reactor.core.publisher.Flux;

/**
 * 2장 커스텀 어드바이저: LLM 응답 소요 시간을 측정해 출력.
 *
 *  - CallAdvisor 와 StreamAdvisor 를 동시에 구현 (동기/스트리밍 모두 지원)
 *  - ChatModel 호출 전후를 감싸는 가장 기본적인 관찰(Observer) 패턴
 */
public class ElapsedTimeAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        System.out.printf("%n[응답 시간] %dms%n", System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.currentTimeMillis();
        return chain.nextStream(request)
                .doOnComplete(() -> System.out.printf("%n[응답 시간] %dms%n",
                        System.currentTimeMillis() - start));
    }

    @Override
    public String getName() {
        return "ElapsedTimeAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
