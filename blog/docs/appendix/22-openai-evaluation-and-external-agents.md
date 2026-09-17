---
title: "부록: 오픈AI 전환, AI 평가, 외부 AI 에이전트 연결"
description: "올라마 대신 오픈AI로 예제를 실행하는 설정, 심판 LLM으로 응답을 평가해 에이전트 루프를 보강하는 방법, 외부 AI 에이전트에 이 책의 MCP 서버를 연결하는 방법을 다룹니다."
tags:
  - 부록
---

# 부록: 오픈AI 전환, AI 평가, 외부 AI 에이전트 연결

<div class="post-meta" markdown>
<span class="tier-chip t4">T4 파운데이션</span> <span class="tier-chip tx">횡단 관심사</span> 책 부록 A~C | 예제 [`README.md`](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md)
</div>

책의 본문 실습은 모두 로컬 올라마 모델로 진행합니다. 인터넷 연결이나 추가 비용 없이 따라 할 수 있기 때문입니다. 부록 A~C는 여기서 한 걸음 더 나갑니다. 모델 제공자를 오픈AI로 바꿔 보고, 에이전트가 내놓은 답을 LLM으로 채점하고, 5장과 6장에서 만든 MCP 서버를 클로드 코드나 코덱스 같은 외부 AI 에이전트에 붙여 봅니다.

세 주제는 성격이 다르지만 이미 만든 구조를 크게 바꾸지 않고 확장한다는 점이 같습니다. 모델은 스프링 AI 공통 API 뒤에서 바뀌고, 평가는 생성 흐름과 떨어진 별도 단계로 붙으며, 외부 에이전트는 MCP 표준을 따라 같은 서버에 접속합니다. 이 글은 세 부록의 핵심을 차례로 정리합니다.

## 오픈AI API로 실습 환경 구성하기

PC 사양이 로컬 모델을 돌리기에 빠듯하거나 더 큰 상용 모델의 답을 확인하고 싶다면 오픈AI API로 옮겨 실행할 수 있습니다. 예제는 `ChatClient`, `EmbeddingModel`, `VectorStore`, `ToolCallback` 같은 스프링 AI 추상화에 의존하므로 자바 코드는 대부분 그대로 두고 의존성과 설정만 바꾸면 됩니다. 처음부터 특정 제공자의 SDK에 코드를 묶어 두었다면 모델을 바꾸거나 여러 모델을 함께 쓸 때 고칠 곳이 훨씬 많았을 것입니다.

먼저 오픈AI 플랫폼에서 결제 정보를 등록하고 API 키를 만듭니다. 테스트 호출이 예상보다 늘어날 수 있으니 월 사용 한도도 정해 둡니다. 발급한 키는 다시 볼 수 없으므로 따로 보관하고, 설정 파일에는 키 값을 적는 대신 `OPENAI_API_KEY` 환경 변수를 참조합니다. 의존성은 `pom.xml`의 올라마 스타터를 오픈AI 스타터로 바꾸거나 둘을 함께 둡니다.

```xml title="pom.xml"
--8<-- "README.md:153:156"
```

설정에서는 스프링 AI 2.0의 모델 선택 키인 `spring.ai.model.chat`, `spring.ai.model.embedding` 값을 `ollama`에서 `openai`로 바꾸고, `spring.ai.openai` 아래에 키와 모델 이름을 적습니다.

```yaml title="application.yml"
--8<-- "README.md:162:172"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md#openai-api로-실행하기)</span>

임베딩 설정은 RAG를 쓰는 `chapter3`, `chapter5`, `chapter6`에만 필요하고, 채팅만 하는 `basic-chat`, `chapter2`, `chapter4`는 `chat` 쪽만 바꾸면 됩니다. 클라이언트와 서버가 나뉜 5장과 6장은 서버 쪽 설정(5장은 `application.yml`의 서버 모드 문서, 6장은 `application-knowledge.yml`)에도 오픈AI 설정을 넣습니다. `gpt-4.1-nano`와 `text-embedding-3-small`은 가장 성능이 좋아서가 아니라 실습에 맞는 균형을 보고 고른 조합입니다. 응답 지연과 비용이 예측 가능하고, 스트리밍부터 구조화한 출력, 툴 호출, RAG까지 두루 소화합니다. gpt-5 계열도 동작하지만 추론 토큰 과금과 지연이 있어 README는 실습용으로 gpt-4 계열을 권합니다.

전환할 때 놓치기 쉬운 곳은 기동 단계입니다. 오픈AI 스타터를 넣으면 채팅과 임베딩 말고도 오디오 같은 오픈AI 자동 구성이 함께 켜지고, 그중 일부 빈이 시작 시점에 자격 증명을 요구합니다. 그래서 오픈AI 채팅을 쓰지 않는 프로세스라도 키가 없으면 뜨지 않습니다. 5장과 6장의 MCP 서버를 띄울 때도 `OPENAI_API_KEY`를 설정해 두고, 기동이 실패하면 환경 변수부터 확인합니다. 각 장 README에는 실제로 전환해 보며 확인한 주의점도 정리되어 있습니다.

- **RAG 유사도 임계값**: 5장과 6장 서버의 0.50은 `bge-m3`에 맞춘 값이라 `text-embedding-3-small`에서는 검색 결과가 비기 쉽습니다. 0.3 정도로 낮추기를 권합니다.
- **스트리밍과 툴 호출**: 4장 최종 CLI처럼 둘을 함께 쓰는 예제는 오픈AI에서 오류가 나므로 비스트리밍 단계 예제로 확인합니다.
- **저수준 호출**: 툴 실행 루프를 직접 제어하는 4장 `ch4-step4` 예제는 `ChatClient`를 거치지 않고 `ChatModel`을 부르므로 옵션 객체도 `OpenAiChatOptions`로 바꿔야 합니다. 제공자 중립 옵션을 제공자별 옵션으로 옮기는 변환이 `ChatClient` 계층에서 일어나기 때문입니다.

## AI 평가로 응답 품질 검증하기

에이전트가 끝까지 실행됐다고 해서 답이 좋다는 보장은 없습니다. 무엇이 맞고 무엇이 틀렸는지 재지 않으면 고칠 방향도 정할 수 없습니다. 부록 B가 소개하는 방법은 LLM as a Judge(심판 LLM)입니다. 평가 역할을 맡은 LLM이 응답을 읽고 질문 의도에 맞는지, 검색한 근거와 어긋나지 않는지, 근거에 없는 내용을 지어내지 않았는지 판정합니다. 사람이 읽고 판단하듯 의미와 사실 관계를 따질 수 있어서 RAG와 에이전트의 품질 평가에 주로 쓰입니다.

스프링 AI의 평가 기능은 `Evaluator` 인터페이스에서 출발합니다. 이 인터페이스의 `evaluate` 메서드가 `EvaluationRequest`를 받아 `EvaluationResponse`를 돌려줍니다. 요청에는 사용자 질문, 근거 문서 목록, 평가할 모델 응답을 담고, 결과에서는 통과 여부 `isPass()`, 0.0~1.0 사이의 점수 `getScore()`, 평가자가 남긴 설명 `getFeedback()`을 꺼냅니다. 구조가 이렇게 작아서 평가를 생성 흐름에서 떼어 원하는 곳에 붙일 수 있습니다. RAG 답변 직후에 둘 수도, 통합 테스트에서만 돌릴 수도, 운영 중 위험도가 높은 요청에만 적용할 수도 있습니다. 결과도 기록으로 끝나지 않습니다. 통과 여부는 품질 게이트가 되고, 점수는 회귀 확인과 실험 비교에 쓰입니다. 다만 기본 평가기 둘은 피드백 문구를 비워 둔 채 돌려주고 관련성 평가의 점수도 0 또는 1이라, 개선 지침이 필요하면 평가 프롬프트를 직접 짠 `Evaluator` 구현이 필요합니다.

기본 평가기는 두 가지이며, 둘 다 평가용 프롬프트로 LLM이 채점하므로 만들 때 심판 모델이 될 `ChatClient.Builder`를 넘깁니다. `RelevancyEvaluator`는 질문, 근거, 답이 서로 맞물리는지 봅니다. 키워드만 비슷한 문서가 검색되면 문장은 매끄러워도 내용이 빗나가는데, 이런 답을 잡아 검색 개수를 늘리거나 검색어를 고쳐 다시 찾게 할 수 있습니다. `FactCheckingEvaluator`는 답 속의 주장이 근거 문서로 실제 뒷받침되는지 봅니다. 맞는 문서를 찾았더라도 모델이 문서에 없는 개정 날짜를 덧붙이면, 관련성 평가는 통과해도 사실성 평가에서 걸릴 수 있습니다. 그래서 RAG에서는 두 평가를 함께 보는 편이 안전합니다.

평가는 모델을 다시 호출하므로 단위 테스트보다 통합 테스트에 어울립니다. 예제 저장소도 라이브 모델이 필요한 검증은 `*IT` 통합 테스트로 나눠 두었고, 응답 품질은 출력을 사람이 보고 판단하게 했습니다. 운영에서는 대표 질문 세트를 통과한 프롬프트만 배포하거나 중요한 응답을 표본으로 뽑아 야간 배치로 평가하는 식으로 자동화할 수 있습니다. 판정도 LLM이 내리므로 작은 로컬 모델에 맡기면 결과가 흔들릴 수 있습니다. 생성 모델과 평가 모델을 분리하고 평가에는 더 안정적인 모델을 두는 편이 좋습니다.

## 평가로 에이전트 루프 보강하기

채점을 사후 검수에서 루프 안으로 옮기면 [6장 워크플로 패턴](../part6/15-agent-loop-and-context-engineering.md)의 평가자-최적화 워크플로가 됩니다. 생성자가 답을 내면 평가자가 기준에 비춰 심사하고, 통과하지 못하면 피드백을 붙여 생성자에게 되돌립니다. RAG처럼 근거에 맞는지가 통과 기준인 작업이라면 평가자 프롬프트를 새로 짜지 않고 `RelevancyEvaluator`나 `FactCheckingEvaluator`를 판정기로 끼울 수 있습니다.

흐름은 단순합니다. 에이전트가 먼저 답을 만들고, 평가기가 질문과 근거 문서와 답을 받아 판정합니다. `isPass()`가 참이면 멈추고, 거짓이면 실패 이유를 담은 지시를 질문에 덧붙여 에이전트를 다시 실행합니다. 기본 평가기는 피드백을 비워 두므로 이 지시는 직접 만든 평가기의 `getFeedback()`이나 미리 정한 문구로 넣습니다. 반복 횟수는 정해 둔 최대 시도 횟수를 넘지 않게 합니다.

이렇게 하면 루프가 끝나는 기준이 바뀝니다. 더 부를 툴이 없어서 멈추는 것이 아니라 결과가 목표에 닿았는지 보고 멈춥니다. 목표에 못 미치면 다시 검색하거나, 다른 툴을 부르거나, 사용자에게 정보를 더 묻게 할 수도 있습니다. 루프를 모델의 판단에만 맡기지 않고 시스템이 함께 제어하는 셈입니다.

대가도 있습니다. 반복마다 생성과 평가가 모두 일어나 호출 수와 토큰, 응답 시간이 함께 늘어납니다. 번역 다듬기, 보고서 초안 개선, 여러 번 검색해야 하는 조사, 코드 생성과 리뷰처럼 되풀이할수록 나아지는 작업에 어울리고, 한 번에 답이 나오는 질의응답에는 과할 수 있습니다. [툴 루프에 반복 상한을 둔 것](../part6/16-recursive-advisor-and-tool-loop-control.md)처럼 최대 시도 횟수, 최소 통과 점수, 고위험 작업에만 적용하는 조건으로 멈출 지점을 분명히 정해 둡니다.

## 외부 AI 에이전트에 이 책의 MCP 서버 연결하기

5장과 6장의 MCP 서버는 Streamable HTTP로 열려 있어서, 책의 CLI 클라이언트가 아니어도 MCP를 지원하는 에이전트라면 같은 서버에 접속할 수 있습니다. 이 실습의 요점은 연결에 성공하는 것보다, 책의 서버가 특정 제품에 묶이지 않은 표준 구현이라는 점을 직접 확인하는 데 있습니다. 성격이 다른 클라이언트 여럿에 같은 서버를 붙여 보면 MCP가 말하는 상호운용성이 무엇인지 드러납니다.

연결할 수 있는 서버는 셋입니다. 5장 RAG 서버는 검색과 근거 답변을, 6장 지식 서버는 `rag_answer_question`을, 6장 운영 서버는 `check_stock`과 `place_purchase_order`를 제공합니다. RAG 계열 서버는 기동하면서 문서를 색인하므로 올라마 `bge-m3` 임베딩 모델이 있어야 합니다. 실습은 다른 서버와 포트가 겹치지 않는 지식 서버(8086)로 진행합니다.

```bash
# 6장 지식 MCP 서버 실행 (엔드포인트 http://localhost:8086/mcp)
cd chapter6
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=knowledge"

# 다른 터미널에서 클로드 코드에 등록
claude mcp add --transport http book-knowledge http://localhost:8086/mcp
```

클로드 코드는 원격 HTTP 서버를 `claude mcp add --transport http` 명령으로 등록합니다. 이름은 자유지만 역할이 드러나는 book-knowledge 같은 이름이 관리하기 편합니다. 등록 목록은 `claude mcp list`로, 연결 상태는 대화 중 `/mcp`로 확인합니다. 이제 "재고 정책 문서에서 안전재고 기준을 찾아줘"처럼 물으면 클로드 코드가 서버의 툴을 찾아 호출하고 그 결과로 답합니다. 6장에서 만든 모델, 툴, 루프의 구조가 외부 에이전트에서도 같은 방식으로 돌아가는 것입니다.

다른 클라이언트도 주소를 알려 주는 방법만 다릅니다. 클로드 데스크톱은 `claude_desktop_config.json`에 `mcp-remote` 브리지를 등록하고 앱을 다시 시작합니다. Settings > Connectors의 커스텀 커넥터는 이 실습의 경로가 아닙니다. 내 컴퓨터가 아니라 앤트로픽 인프라에서 서버에 접속하므로 localhost 주소로는 연결되지 않습니다. 오픈클로는 `openclaw mcp set` 명령에 서버 이름과 함께 URL, 전송 방식을 담은 JSON을 넘깁니다. 코덱스는 CLI와 IDE 확장이 같은 설정을 공유합니다. `~/.codex/config.toml`이나 프로젝트의 `.codex/config.toml`에 URL을 적으면 되고, 버전에 따라 `codex mcp add` 명령으로도 등록할 수 있습니다.

```toml title="~/.codex/config.toml"
--8<-- "README.md:113:114"
```
<span class="code-link">[전체 코드 보기](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md#mcp-서버를-외부-ai-클라이언트에-연결하기)</span>

연결할 때 챙길 점도 있습니다. 5장 서버와 6장 운영 서버는 둘 다 8085 포트라 동시에 띄우지 않습니다. 운영 서버에서 `place_purchase_order`를 호출하면 주문에 앞서 MCP 추가 정보 요청(Elicitation)으로 [사용자 승인](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)을 묻습니다. 이 기능을 지원하지 않는 클라이언트라면 `check_stock` 같은 조회 툴로 먼저 확인합니다. 예제 서버는 인증 없이 모든 네트워크 인터페이스에서 요청을 받으므로, 로컬 실습만 할 때는 `--server.address=127.0.0.1`로 루프백에만 묶을 수 있고, 다른 네트워크에서 접근하게 하려면 [MCP 보안 구성](../part5/13-mcp-security-oauth2-and-jwt.md)을 먼저 적용합니다. 서버와 클라이언트가 같은 머신에 있지 않거나 컨테이너로 나뉘어 있다면 주소의 localhost를 클라이언트에서 접근할 수 있는 호스트 이름으로 바꿉니다.

## 4-티어 아키텍처에서의 위치

부록 A는 T4 파운데이션의 모델을 바꾸는 작업입니다. 위쪽의 오케스트레이션(T2)과 능력(T3)이 스프링 AI 공통 API에 기대고 있어 제공자가 바뀌어도 대부분 의존성과 설정 변경으로 끝납니다. 부록 B는 평가로 에이전트가 만든 응답의 품질을 검증하고, 그 판정을 반복을 멈추는 기준과 다음 시도의 입력으로 삼아 에이전트 루프를 보강합니다. 부록 C에서는 사용자가 만나는 T1 채널이 책의 CLI에서 클로드 코드나 코덱스로 바뀌고, 툴을 고르는 루프도 외부 에이전트가 맡습니다. 그래도 능력 호출 표준 인터페이스인 MCP가 사이에 있어 T3 자리의 MCP 서버는 그대로 쓸 수 있습니다. 연결할 때 챙긴 승인 게이트와 보안 구성은 책이 횡단 관심사로 묶은 거버넌스와 보안에 속합니다. 표준에 맞춘 서버를 연결해 한 화면에서 검사하고 시험하는 별도 오픈소스 도구는 다음 글 [부록: 스프링 AI 플레이그라운드](../appendix/23-spring-ai-playground.md)에서 소개합니다.

## 책에서 더 다루는 내용

!!! book "책 부록 A~C"
    - 부록 A: API 키 발급과 사용 한도 설정 순서, 키를 코드와 분리해 다루는 습관
    - 부록 B: `RelevancyEvaluator`, `FactCheckingEvaluator`로 평가 요청을 보내는 예제 코드와 `EvaluationResponse` 값 정리
    - 부록 B: 평가기를 판정기로 넣은 평가자-최적화 루프 예제 코드
    - 부록 C: 클로드 데스크톱의 커넥터 방식과 `claude_desktop_config.json` 구성 예
    - 부록 C: 코덱스와 오픈클로를 함께 소개하는 이유와 연결한 뒤 각 클라이언트에서 활용하는 방향

    [책 소개](../book.md){ .md-button } [온라인 구매](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## 참고 자료

- [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html): 스프링 AI 평가 API 레퍼런스
- [Evaluator-Optimizer Pattern](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns/evaluator-optimizer): 스프링 AI 예제 저장소의 평가자-최적화 예제
- [Connect Claude Code to tools via MCP](https://code.claude.com/docs/en/mcp): 클로드 코드의 MCP 서버 연결 문서
- [Get started with custom connectors using remote MCP](https://support.claude.com/en/articles/11175166-get-started-with-custom-connectors-using-remote-mcp): 원격 MCP 기반 커스텀 커넥터 안내
- [Connectors overview](https://claude.com/docs/connectors/overview): 클로드 커넥터 개요
- [Model Context Protocol - Codex](https://developers.openai.com/codex/mcp): 코덱스의 MCP 설정 문서
- [OpenClaw Docs - MCP](https://docs.openclaw.ai/cli/mcp): 오픈클로의 MCP 명령 문서
