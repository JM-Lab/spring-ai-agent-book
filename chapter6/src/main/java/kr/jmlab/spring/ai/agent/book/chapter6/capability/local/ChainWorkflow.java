package kr.jmlab.spring.ai.agent.book.chapter6.capability.local;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 프롬프트 체이닝(Prompt Chaining) 워크플로우.
 *
 * <p>하나의 작업을 여러 단계로 나누고, 이전 단계의 출력을 다음 단계의 입력으로 넘겨 순차적으로 처리한다.
 * 각 단계는 자신만의 시스템 프롬프트를 가지며, 단계가 누적될수록 결과가 정교해진다.</p>
 */
public class ChainWorkflow {

    private final ChatClient chatClient;
    private final String[] systemPrompts;

    public ChainWorkflow(ChatClient chatClient, String[] systemPrompts) {
        this.chatClient = chatClient;
        this.systemPrompts = systemPrompts;
    }

    public String chain(String userInput) {
        String response = userInput;

        // 각 시스템 프롬프트를 순차적으로 적용
        for (String prompt : systemPrompts) {
            String input = String.format("{%s}\n {%s}", prompt, response);
            // 이전 단계의 결과를 다음 단계의 입력으로 전달하여 체이닝
            response = chatClient.prompt(input).call().content();
        }

        return response; // 최종 처리 결과 반환
    }
}
