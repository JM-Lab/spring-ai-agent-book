---
title: 책 소개
description: 「스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드」(허제민 지음, 위키북스, 2026) 소개, 전체 목차, 실습 환경, 예제 코드, 오류 제보
search:
  boost: 0.5   # 목차 페이지가 글보다 위에 뜨지 않게 검색 가중치를 낮춘다
---

# 스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드

<div class="book-card" markdown>
<div class="cover-wrap"><img class="off-glb" src="../assets/img/cover.jpg" alt="스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드 표지"></div>
<div markdown>
**자바와 스프링으로 만드는 엔터프라이즈 AI 에이전트 시스템**

허제민 지음, 위키북스 오픈소스 & 웹 시리즈, 2026년 9월 17일 출간, 692쪽, ISBN 9791158396961

[온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary } [예제 코드](https://github.com/JM-Lab/spring-ai-agent-book){ .md-button }
</div>
</div>

## 어떤 책인가요

AI 에이전트는 질문에 답하는 챗봇을 넘어, 기업 문서를 검색하고 업무 시스템의 툴을 호출하며 여러 단계를 거쳐 실제 작업을 수행하는 시스템으로 발전하고 있습니다. 이런 시스템을 실제 서비스에 적용하려면 모델의 답변 품질만으로는 부족합니다. 기업 데이터에 대한 접근 권한을 통제하고, 툴 실행을 안전하게 관리하고, 중요한 작업에는 사람의 승인을 거치고, 실행 과정 전체를 관측하고 평가할 수 있어야 합니다.

이 책은 스프링 AI 2.0으로 LLM 채팅, RAG, 툴 호출, MCP, AI 에이전트를 기술이 발전해 온 순서대로 설명합니다. 자바와 스프링에 익숙한 개발자가 지금의 애플리케이션 구조를 유지하면서 엔터프라이즈 AI 에이전트 시스템을 만들 수 있도록 안내합니다.

## 챗 CLI 하나를 끝까지 키워 가는 구성

기능별 예제를 나열하지 않고, 챗 CLI 프로젝트 하나를 장마다 확장합니다. `ChatClient`로 대화를 시작하고, RAG와 벡터 데이터베이스를 더해 기업 문서를 활용하고, 툴 호출과 MCP로 외부 시스템을 연결합니다. 마지막에는 AI 에이전트 서비스를 채널, 오케스트레이션, 능력, 파운데이션의 4-티어 아키텍처로 정의하고, 그 설계에 따라 파일 시스템과 MCP 툴, 툴 검색 메타 툴을 결합해 필요한 기능을 스스로 골라 쓰는 AI 에이전트 CLI를 완성합니다.

모든 실습은 올라마 기반 로컬 LLM으로 진행하므로 API 키나 사용 비용 부담 없이 따라 할 수 있습니다. 오픈AI 모델로 전환하는 방법도 부록에서 다룹니다.

## 이 책에서 다루는 내용

- LLM과 AI 에이전트의 핵심 개념부터 스프링 AI 2.0 기반 AI 애플리케이션 개발까지
- 프롬프트 엔지니어링, 구조화한 출력, 대화 메모리, 어드바이저 체인
- 문서 수집(extract), 변환(transform), 적재(load)와 벡터 데이터베이스
- 기본 RAG와 고급 RAG 설계 및 구현
- 자바 메서드와 외부 시스템을 연결하는 툴 호출
- MCP(Model Context Protocol) 클라이언트와 서버 구현
- 툴 검색 메타 툴과 MCP 툴을 활용한 에이전트 능력 확장
- 에이전트 스킬과 하위 에이전트를 활용하는 AI 에이전트 구현
- 채널, 오케스트레이션, 능력, 파운데이션으로 구성한 AI 에이전트 서비스 4-티어 아키텍처
- 사람의 승인, 평가와 검증, 관측 가능성을 갖춘 엔터프라이즈 AI 에이전트 시스템

## 이런 분께 권합니다

자바와 스프링으로 애플리케이션을 개발해 본 개발자를 위한 책입니다. AI나 머신러닝 경험은 없어도 됩니다. LLM의 동작 원리부터 차근차근 설명하므로, 스프링으로 웹 애플리케이션이나 REST API를 만들어 봤다면 충분히 따라올 수 있습니다.

## 실습 환경

| 항목 | 기준 |
| --- | --- |
| 자바 | 21 |
| 스프링 부트 | 4.0.x |
| 스프링 AI | 2.0.0 GA |
| 채팅 모델 | 올라마 `qwen3.5:4b` |
| 임베딩 모델 | 올라마 `bge-m3` |

## 전체 목차

장 제목을 누르면 절 목록이 펼쳐집니다. 장마다 그 장을 정리한 이 사이트의 글도 함께 연결했습니다.

??? abstract "1장 AI 에이전트, 새로운 패러다임의 시작"
    - 1.1 LLM을 넘어, 스스로 일하는 ‘에이전트’의 시대로
    - 1.2 기업 환경에서의 AI 기술 도입
        - 1.2.1 파이썬 중심의 AI 개발 생태계
        - 1.2.2 자바 AI 개발 생태계의 전환: 새로운 가능성의 시작
    - 1.3 스프링 AI 소개
        - 1.3.1 스프링 AI의 목표
        - 1.3.2 스프링 생태계에서 스프링 AI의 역할과 장점
        - 1.3.3 스프링 AI의 진화: AI 통합의 1.0에서 에이전트의 2.0으로
    - 1.4 이 책의 목표와 구성
        - 1.4.1 자바 개발자를 위한 실용적인 AI 에이전트 개발 가이드

    이 사이트의 글: [LLM 호출에서 AI 에이전트로, 그리고 왜 자바 개발자는 스프링 AI인가](part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md), [스프링 AI 1.0에서 2.0으로, 무엇이 달라졌나](part1/02-whats-new-in-spring-ai-2-0.md)

??? abstract "2장 스프링 AI 프레임워크"
    - 2.1 주요 특징과 AI 모델 API 설계 방향
        - 2.1.1 주요 특징
        - 2.1.2 AI Model API 설계
    - 2.2 개발 환경 구축
        - 2.2.1 올라마 설치 및 로컬 환경 구성
        - 2.2.2 프로젝트 기본 설정
        - 2.2.3 간단한 AI 애플리케이션 개발
    - 2.3 ChatModel과 ChatClient
        - 2.3.1 역할의 분리: 드라이버와 클라이언트
        - 2.3.2 ChatModel: 통신 방식과 응답 데이터
        - 2.3.3 ChatResponse와 응답 메타데이터
        - 2.3.4 ChatClient API 활용
        - 2.3.5 ChatClient의 고급 기능과 AI 에이전트로의 확장
    - 2.4 프롬프트 엔지니어링
        - 2.4.1 Prompt 설계 철학
        - 2.4.2 Message API와 역할 기반 설계
        - 2.4.3 PromptTemplate: 동적 프롬프트 관리
        - 2.4.4 고급 템플릿 기법
        - 2.4.5 효과적인 프롬프트 작성
        - 2.4.6 ChatOptions 활용
        - 2.4.7 모델 전용 옵션 활용과 설정 전략
        - 2.4.8 다양한 프롬프트 엔지니어링 기법
    - 2.5 토큰
        - 2.5.1 토큰의 구조와 개발자의 관점
        - 2.5.2 한국어 토큰 효율성의 진화
        - 2.5.3 하이브리드 프롬프트 전략
    - 2.6 구조화한 출력
        - 2.6.1 프레임워크 주도 프롬프트 엔지니어링
        - 2.6.2 구조화한 출력의 아키텍처
        - 2.6.3 StructuredOutputConverter 구현체
        - 2.6.4 다양한 StructuredOutputConverter 구현체 사용 방법
        - 2.6.5 네이티브 구조화한 출력
    - 2.7 대화 메모리와 컨텍스트 관리
        - 2.7.1 ChatMemory 인터페이스와 기본 구현체
        - 2.7.2 ChatMemoryRepository 저장소 유형별 상세 분석
        - 2.7.3 ChatMemoryRepository의 유연한 저장소 전환
        - 2.7.4 단기 기억과 장기 기억의 연동 패턴
        - 2.7.5 수동 단기 기억 관리
    - 2.8 어드바이저 체인
        - 2.8.1 어드바이저 API 클래스 구조
        - 2.8.2 어드바이저 실행 흐름과 스택 구조
        - 2.8.3 동기식 처리 vs 스트리밍 처리
        - 2.8.4 어드바이저 구현과 내장 어드바이저
        - 2.8.5 어드바이저 기반 제어: 차단과 대체
        - 2.8.6 어드바이저 구현 원칙과 배치 전략
        - 2.8.7 스프링 AI 어드바이저: 프레임워크의 코어와 확장성
    - 2.9 AI 챗봇 CLI 프로젝트
        - 2.9.1 CLI와 AI의 만남
        - 2.9.2 프로젝트 구조 및 설정
        - 2.9.3 단계별 챗 CLI 구현
        - 2.9.4 최종 CLI 챗봇 개발
        - 2.9.5 정리

    이 사이트의 글: [ChatModel과 ChatClient, 그리고 프롬프트 엔지니어링](part2/03-chatmodel-chatclient-and-prompt-engineering.md), [토큰과 구조화한 출력: 모델의 답을 자바 객체로 받기](part2/04-tokens-and-structured-output.md), [대화 메모리와 어드바이저 체인: 스프링 AI의 코어](part2/05-chat-memory-and-advisor-chain.md)

??? abstract "3장 스프링 AI와 RAG"
    - 3.1 RAG 등장 배경과 아키텍처
        - 3.1.1 LLM의 구조적 한계와 데이터 공백
        - 3.1.2 데이터 공백을 채우는 기술: 파인 튜닝 vs 주입
        - 3.1.3 스프링 AI의 RAG 아키텍처
        - 3.1.4 ETL 파이프라인 프레임워크
    - 3.2 ETL 파이프라인 DocumentReader 구현
        - 3.2.1 텍스트 파일에서 추출
        - 3.2.2 JSON에서 추출
        - 3.2.3 PDF에서 추출
        - 3.2.4 마크다운에서 추출
        - 3.2.5 HTML에서 추출
        - 3.2.6 범용 문서 추출
        - 3.2.7 커스텀 DocumentReader 구현
    - 3.3 ETL 파이프라인 DocumentTransformer 구현
        - 3.3.1 문서 변환과 정제
        - 3.3.2 메타데이터 포맷 통일
        - 3.3.3 메타데이터에 키워드 추가
        - 3.3.4 문맥 요약 추가
        - 3.3.5 커스텀 DocumentTransformer 구현
        - 3.3.6 DocumentTransformer 파이프라인 통합
    - 3.4 ETL 파이프라인 DocumentWriter 구현
        - 3.4.1 파일에 적재
        - 3.4.2 커스텀 DocumentWriter 구현
    - 3.5 임베딩 모델
        - 3.5.1 임베딩의 개념과 작동 원리
        - 3.5.2 EmbeddingModel API 구조
        - 3.5.3 임베딩 모델 구동 방식: 클라우드와 로컬
        - 3.5.4 CPU 기반 로컬 임베딩 모델 성능과 한글 지원 임베딩 모델
    - 3.6 벡터 데이터베이스
        - 3.6.1 VectorStore 인터페이스 상세
        - 3.6.2 검색 요청(SearchRequest)
        - 3.6.3 메타데이터 필터
        - 3.6.4 벡터 데이터베이스 스키마
        - 3.6.5 대량 문서의 배치 데이터 적재
        - 3.6.6 벡터 데이터베이스의 데이터 생명 주기 관리(삭제와 수정)
        - 3.6.7 다양한 벡터 데이터베이스
        - 3.6.8 내장 인메모리 벡터 데이터베이스(SimpleVectorStore)
    - 3.7 스프링 AI의 RAG 프레임워크
        - 3.7.1 RAG의 진화와 패러다임
        - 3.7.2 Naive RAG 구현
        - 3.7.3 Advanced RAG 구현
        - 3.7.4 모듈러 RAG를 구성하는 모듈
        - 3.7.5 모듈러 RAG의 전처리 단계 구현
        - 3.7.6 모듈러 RAG의 검색, 후처리, 생성 단계 구현
        - 3.7.7 모듈러 RAG 오케스트레이션
    - 3.8 RAG AI 챗봇 CLI 프로젝트
        - 3.8.1 프로젝트 목표와 설정
        - 3.8.2 오프라인 ETL 파이프라인 구현
        - 3.8.3 런타임 RAG 파이프라인 구현
        - 3.8.4 RAG CLI 챗봇 개발

    이 사이트의 글: [RAG 아키텍처와 ETL 파이프라인: 문서를 AI의 지식으로](part3/06-rag-architecture-and-etl-pipeline.md), [임베딩 모델과 벡터 데이터베이스](part3/07-embedding-models-and-vector-stores.md), [Naive RAG에서 모듈러 RAG로: 스프링 AI RAG 프레임워크](part3/08-from-naive-rag-to-modular-rag.md)

??? abstract "4장 툴 호출"
    - 4.1 AI 모델의 툴 호출 환경
        - 4.1.1 툴 호출의 등장: LLM이 현실 세계와 연결되다
    - 4.2 툴 호출 설계
        - 4.2.1 스프링 AI 툴 호출의 특징
        - 4.2.2 툴 호출 진행 과정
        - 4.2.3 툴 명세
        - 4.2.4 툴 컨텍스트와 직접 반환
        - 4.2.5 툴 호출 결과 변환
    - 4.3 툴 구현
        - 4.3.1 메서드를 툴로 구현하기
        - 4.3.2 함수를 툴로 구현하기
        - 4.3.3 툴 구현 방식 비교 및 선택
        - 4.3.4 툴을 AI 모델에 전달하는 방법
    - 4.4 툴 실행
        - 4.4.1 ToolCallingManager API
        - 4.4.2 DefaultToolCallingManager의 내부 구현
        - 4.4.3 프레임워크-제어 툴 실행
        - 4.4.4 어드바이저-제어 툴 실행
        - 4.4.5 사용자가 제어하는 툴 실행
    - 4.5 툴 지원 AI 챗봇 CLI 프로젝트
        - 4.5.1 프로젝트 목표와 설정
        - 4.5.2 단계별 툴 호출 CLI 구현
        - 4.5.3 툴 지원 AI 챗봇 CLI 개발

    이 사이트의 글: [툴 호출 설계: LLM이 현실 세계와 연결되는 방법](part4/09-tool-calling-design-in-spring-ai.md), [툴 구현과 실행 제어: @Tool에서 ToolCallingManager까지](part4/10-tool-implementation-and-execution-control.md)

??? abstract "5장 스프링 AI MCP"
    - 5.1 MCP 기본(호스트/클라이언트/서버, 프리미티브, 전송)
        - 5.1.1 MCP 아키텍처
        - 5.1.2 자바 MCP 스택 아키텍처
        - 5.1.3 자바 MCP 클라이언트 서버 아키텍처
        - 5.1.4 직접 툴 호출 vs MCP 툴 호출
    - 5.2 스프링 AI MCP 클라이언트
        - 5.2.1 MCP 클라이언트 부트 스타터
        - 5.2.2 MCP 클라이언트 공통 설정
        - 5.2.3 MCP 클라이언트 STDIO 설정
        - 5.2.4 MCP 클라이언트 원격 서버 설정
        - 5.2.5 MCP 클라이언트의 headers를 사용한 원격 서버 보안 설정
        - 5.2.6 MCP 클라이언트 기능 구현
        - 5.2.7 MCP 클라이언트 애너테이션
    - 5.3 스프링 AI MCP 서버
        - 5.3.1 MCP 서버 부트 스타터
        - 5.3.2 MCP 서버 공통 설정
        - 5.3.3 MCP 서버 프로토콜 설정
        - 5.3.4 MCP 서버 툴 기능 구현
        - 5.3.5 MCP 서버 다양한 기능 구현
        - 5.3.6 MCP 서버 클라이언트 양방향 기능 구현
        - 5.3.7 MCP 서버 애너테이션
        - 5.3.8 MCP 서버 애너테이션 사용 툴 구현
        - 5.3.9 MCP 서버 애너테이션 사용 툴 서버 유형별 구현
        - 5.3.10 MCP 서버 애너테이션 사용 추가 기능 구현
        - 5.3.11 MCP 서버 애너테이션 사용 추가 기능 서버 유형별 구현
    - 5.4 스프링 AI MCP 보안
        - 5.4.1 MCP 보안 환경
        - 5.4.2 스프링 AI의 MCP 시큐리티
        - 5.4.3 MCP 클라이언트 시큐리티 공통 설정
        - 5.4.4 MCP 클라이언트 시큐리티 세부 구현
        - 5.4.5 MCP 서버 시큐리티 구성
        - 5.4.6 MCP 인가 서버 구성
    - 5.5 MCP 기반 AI 챗봇 CLI 프로젝트
        - 5.5.1 프로젝트 목표와 설정
        - 5.5.2 단계별 MCP 서버 구현
        - 5.5.3 단계별 MCP 클라이언트 구현
        - 5.5.4 MCP 기반 AI 챗봇 CLI 구현

    이 사이트의 글: [MCP 기본과 스프링 AI MCP 클라이언트](part5/11-mcp-basics-and-spring-ai-mcp-client.md), [스프링 AI로 MCP 서버 만들기: 부트 스타터부터 애너테이션까지](part5/12-building-an-mcp-server-with-spring-ai.md), [MCP 보안: OAuth2와 JWT로 지키는 MCP 클라이언트와 서버](part5/13-mcp-security-oauth2-and-jwt.md)

??? abstract "6장 AI 에이전트"
    - 6.1 AI 에이전트와 실행 루프
        - 6.1.1 워크플로와 자율 에이전트
        - 6.1.2 워크플로 패턴
        - 6.1.3 자율 에이전트
        - 6.1.4 스프링 AI가 지원하는 자율 에이전트 개발
        - 6.1.5 실용적 AI 에이전트 구현
    - 6.2 컨텍스트 엔지니어링
        - 6.2.1 프롬프트 엔지니어링에서 컨텍스트 엔지니어링으로
        - 6.2.2 생각의 사슬의 프롬프트 설계와 추론 AI 모델
        - 6.2.3 리액트: 에이전트 행동 방식
        - 6.2.4 컨텍스트 엔지니어링을 사용하는 LLM과 에이전트 시스템
    - 6.3 재귀적 어드바이저와 툴 루프 제어
        - 6.3.1 툴 루프의 두 가지 방식
        - 6.3.2 재귀적 어드바이저의 처리 흐름
        - 6.3.3 ToolCallingAdvisor를 사용한 툴 루프 제어
        - 6.3.4 ToolCallingAdvisor의 훅과 파라미터 증강
        - 6.3.5 구조화한 출력 교정 루프
    - 6.4 스프링 AI 에이전트 개발
        - 6.4.1 AI 에이전트 서비스 구조: 클로드 코드, 코덱스, 오픈코드, 오픈클로
        - 6.4.2 멀티 에이전트와 에이전트 연결 표준
        - 6.4.3 스프링 AI 에이전트 아키텍처와 구현
        - 6.4.4 사용자 개입 워크플로(HITL): MCP Elicitation을 통한 툴 사용 권한 제어
        - 6.4.5 동적 툴 탐색
    - 6.5 커뮤니티 지원으로 확장하는 스프링 AI 에이전트
        - 6.5.1 범용 에이전트 스킬
        - 6.5.2 가정 대신 질문하는 툴(AskUserQuestionTool)
        - 6.5.3 작업 계획 작성 툴(TodoWriteTool)
        - 6.5.4 멀티 에이전트 지원 하위 에이전트 오케스트레이션
    - 6.6 엔터프라이즈 스프링 AI 에이전트 CLI 프로젝트
        - 6.6.1 엔터프라이즈 스프링 AI 에이전트 시스템
        - 6.6.2 엔터프라이즈 스프링 AI 에이전트 CLI 시스템 설계와 구현 계획
        - 6.6.3 스프링 AI 에이전트 CLI 구현
        - 6.6.4 MCP 서버 기반 스프링 AI 에이전트 능력 확장 구현
        - 6.6.5 멀티 에이전트로 확장
        - 6.6.6 메타 툴 기반 오케스트레이션 확장
        - 6.6.7 통합과 관측 가능성

    이 사이트의 글: [AI 에이전트 서비스 4-티어 아키텍처](part6/14-four-tier-architecture-for-ai-agent-systems.md), [에이전트 실행 루프와 컨텍스트 엔지니어링](part6/15-agent-loop-and-context-engineering.md), [재귀적 어드바이저와 툴 루프 제어](part6/16-recursive-advisor-and-tool-loop-control.md), [스프링 AI 에이전트 아키텍처와 구현, 그리고 동적 툴 탐색](part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md), [사용자 개입(HITL): MCP Elicitation으로 만드는 승인 게이트](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md), [에이전트 스킬: 범용 스킬로 에이전트의 능력을 확장하기](part6/19-agent-skills-extending-capabilities.md), [엔터프라이즈 스프링 AI 에이전트 CLI: 멀티 에이전트와 메타 툴 오케스트레이션](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md), [AI 에이전트 관측 가능성: Micrometer, OTel GenAI 규약, OTLP](part6/21-observability-for-ai-agents-micrometer-otel-otlp.md)

??? abstract "부록"
    - 부록 A. 오픈AI API로 실습 환경 구성하기
    - 부록 B. AI 평가로 응답 품질을 검증하고 에이전트 루프를 보강하기
    - 부록 C. 외부 AI 에이전트에 연결하기
    - 부록 D. 스프링 AI 플레이그라운드

    이 사이트의 글: [오픈AI 전환, AI 평가, 외부 AI 에이전트 연결](appendix/22-openai-evaluation-and-external-agents.md), [스프링 AI 플레이그라운드](appendix/23-spring-ai-playground.md)

## 예제 코드

장별 실습 프로젝트는 예제 저장소에 있습니다. 각 장 폴더는 독립된 Maven 프로젝트라 폴더 안에서 바로 실행할 수 있습니다.

- 저장소: [github.com/JM-Lab/spring-ai-agent-book](https://github.com/JM-Lab/spring-ai-agent-book)
- ZIP 내려받기: [main.zip](https://github.com/JM-Lab/spring-ai-agent-book/archive/refs/heads/main.zip)

## 오류 제보

책이나 이 사이트에서 잘못된 곳을 발견했거나 예제 실행이 막히면 예제 저장소의 [이슈](https://github.com/JM-Lab/spring-ai-agent-book/issues)로 알려 주세요. 확인한 내용은 해당 글에 반영합니다.
