package kr.jmlab.spring.ai.agent.book.chapter2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Chatbot CLI 프로젝트의 Spring Boot 엔트리포인트.
 *
 * <p>같은 컨텍스트 안에 Ch2Step1 ~ Ch2Step5 러너와 최종 통합 러너가 모두 등록되지만,
 * {@code spring.ai.cli.step} 프로퍼티 값과 일치하는 {@code @ConditionalOnProperty}
 * 빈만 활성화되어 한 번에 하나만 실행된다.</p>
 */
@SpringBootApplication
public class Chapter2Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter2Application.class, args);
    }
}
