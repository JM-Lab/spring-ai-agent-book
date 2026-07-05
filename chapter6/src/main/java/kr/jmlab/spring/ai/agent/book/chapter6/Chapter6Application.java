package kr.jmlab.spring.ai.agent.book.chapter6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 엔터프라이즈 스프링 AI 에이전트 CLI 프로젝트의 Spring Boot 엔트리포인트.
 *
 * <p>하나의 모듈을 세 프로파일로 나눠, 클라이언트와 두 MCP 서버를 별도 프로세스로 실행한다.</p>
 * <ul>
 *   <li>기본 프로파일 {@code client}: 메인 에이전트 CLI(클라이언트). 옵션 없이 실행하면 이 역할로 뜬다.</li>
 *   <li>{@code ops} 프로파일: 운영(주문, 재고) MCP 서버(포트 8085). {@code --spring.profiles.active=ops} 로 띄움.</li>
 *   <li>{@code knowledge} 프로파일: 지식(RAG) MCP 서버(포트 8086). {@code --spring.profiles.active=knowledge} 로 띄움.</li>
 * </ul>
 */
@SpringBootApplication
public class Chapter6Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter6Application.class, args);
    }
}
