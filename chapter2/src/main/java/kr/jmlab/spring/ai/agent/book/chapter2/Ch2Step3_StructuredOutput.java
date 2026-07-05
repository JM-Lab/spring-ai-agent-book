package kr.jmlab.spring.ai.agent.book.chapter2;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 2장 Step 3: 구조화한 출력 (Structured Output).
 *
 * 학습 포인트:
 *  - entity(Class) 호출로 LLM 응답을 자바 record 로 자동 매핑 (리스트 없이 스칼라 필드만으로 단순하게)
 *  - useProviderStructuredOutput() 으로 Ollama 네이티브 구조화 출력(format 강제) 활성화
 *
 * <h3>이 Step 이 {@code .stream()} 이 아닌 {@code .call()} 을 쓰는 이유</h3>
 * 구조화한 출력은 “완성된 JSON” 이 있어야 객체로 파싱 가능합니다. 스프링 AI 의
 * {@code .entity(Class)} 메서드도 {@code CallResponseSpec} 에만 정의되어 있어,
 * 프레임워크 설계 자체가 “구조화한 출력은 call 기반” 임을 가리킵니다. 이 Step 은
 * 2장 CLI 챗봇 전체에서 유일하게 {@code .call()} 을 사용하는 “의도된 예외” 입니다.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch2-step3")
class Ch2Step3_StructuredOutput implements CommandLineRunner {

    record Recipe(
            String dishName,        // 요리 이름
            int cookingMinutes,     // 조리 시간(분)
            String mainIngredient   // 대표 재료
    ) {}

    private final ChatClient chatClient;

    Ch2Step3_StructuredOutput(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch2] Step3: 구조화한 출력 (Recipe, call + entity) ===");
        System.out.println("요리 이름을 입력하세요. 종료: /exit\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("요리명 > ");
                String input = scanner.nextLine();
                if ("/exit".equalsIgnoreCase(input.trim())) break;

                Recipe recipe = chatClient.prompt()
                        .user("'" + input + "' 요리의 정보를 알려줘. 조리 시간(분)과 대표 재료를 알려줘.")
                        .call()
                        // 네이티브 구조화 출력: JSON 스키마를 Ollama 의 format 옵션으로 전달해 응답을 스키마에 맞게 강제
                        .entity(Recipe.class, spec -> spec.useProviderStructuredOutput().validateSchema());

                printRecipe(recipe);
            }
        }
    }

    private void printRecipe(Recipe recipe) {
        System.out.println("\n┌─────────────────────────────────────");
        System.out.println("│ " + recipe.dishName());
        System.out.println("├─ 조리 시간 : " + recipe.cookingMinutes() + "분");
        System.out.println("├─ 대표 재료 : " + recipe.mainIngredient());
        System.out.println("└─────────────────────────────────────\n");
    }
}
