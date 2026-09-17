---
title: Glossary
description: "The site's terms in one table, with the Korean terms from the book and other names they go by. Search for the name you know to find the article on it."
---

# Glossary

This site follows the terminology of the book. If you know something by a different name, find it in this table and follow the link to the article that covers it. The third column lists other names and related search keywords, not only exact synonyms, and the site search indexes them, so searching for a different name brings up this page as well.

| Term on this site | Korean term in the book | Related terms and search keywords | Covered in |
| --- | --- | --- | --- |
| 4-tier architecture | <span lang="ko">4-티어 아키텍처</span> | four-tier architecture, four-layer architecture, layered architecture, n-tier architecture | [4-Tier Architecture for AI Agent Systems](part6/14-four-tier-architecture-for-ai-agent-systems.md) |
| Channel, Orchestration, Capability, Foundation | <span lang="ko">채널, 오케스트레이션, 능력, 파운데이션</span> | T1, T2, T3, T4, tiers, layers | [4-Tier Architecture for AI Agent Systems](part6/14-four-tier-architecture-for-ai-agent-systems.md) |
| cross-cutting concerns | <span lang="ko">횡단 관심사</span> | crosscutting concerns, common concerns, shared concerns | [4-Tier Architecture for AI Agent Systems](part6/14-four-tier-architecture-for-ai-agent-systems.md) |
| observability | <span lang="ko">관측 가능성</span> | o11y, monitoring, telemetry, tracing, LLM observability | [Observability for AI Agents](part6/21-observability-for-ai-agents-micrometer-otel-otlp.md) |
| tool calling | <span lang="ko">툴 호출</span> | function calling, tool use | [Designing Tool Calling](part4/09-tool-calling-design-in-spring-ai.md), [Implementing Tools and Controlling Execution](part4/10-tool-implementation-and-execution-control.md) |
| tool loop | <span lang="ko">툴 루프</span> | tool calling loop, tool call loop, agent loop, agentic loop | [Recursive Advisors and Tool Loop Control](part6/16-recursive-advisor-and-tool-loop-control.md) |
| recursive advisor | <span lang="ko">재귀적 어드바이저</span> | tool loop advisor, ToolCallingAdvisor | [Recursive Advisors and Tool Loop Control](part6/16-recursive-advisor-and-tool-loop-control.md) |
| advisor chain | <span lang="ko">어드바이저 체인</span> | advisor pipeline, interceptor chain, middleware, Advisors API | [Chat Memory and the Advisor Chain](part2/05-chat-memory-and-advisor-chain.md) |
| chat memory | <span lang="ko">대화 메모리</span> | conversation memory, chat history, context memory, ChatMemory, MessageChatMemoryAdvisor | [Chat Memory and the Advisor Chain](part2/05-chat-memory-and-advisor-chain.md) |
| structured output | <span lang="ko">구조화한 출력</span> | JSON output, JSON mode, output converter, BeanOutputConverter | [Tokens and Structured Output](part2/04-tokens-and-structured-output.md) |
| prompt template | <span lang="ko">프롬프트 템플릿</span> | PromptTemplate, prompt engineering, system prompt | [ChatModel, ChatClient, and Prompt Engineering](part2/03-chatmodel-chatclient-and-prompt-engineering.md) |
| embedding model | <span lang="ko">임베딩 모델</span> | embeddings, text embeddings, vectorization, EmbeddingModel, bge-m3 | [Embedding Models and Vector Databases](part3/07-embedding-models-and-vector-stores.md) |
| vector database | <span lang="ko">벡터 데이터베이스</span> | vector store, vector DB, VectorStore, pgvector | [Embedding Models and Vector Databases](part3/07-embedding-models-and-vector-stores.md) |
| ETL pipeline | <span lang="ko">ETL 파이프라인</span> | ingestion pipeline, document ingestion, DocumentReader, chunking, indexing | [RAG Architecture and the ETL Pipeline](part3/06-rag-architecture-and-etl-pipeline.md) |
| Modular RAG | <span lang="ko">모듈러 RAG</span> | Advanced RAG, RAG pipeline, RetrievalAugmentationAdvisor | [From Naive RAG to Modular RAG](part3/08-from-naive-rag-to-modular-rag.md) |
| MCP | <span lang="ko">MCP</span> | Model Context Protocol, MCP server, MCP client, MCP host | [MCP Basics and the Spring AI MCP Client](part5/11-mcp-basics-and-spring-ai-mcp-client.md), [Building an MCP Server with Spring AI](part5/12-building-an-mcp-server-with-spring-ai.md) |
| MCP security | <span lang="ko">MCP 시큐리티</span> | MCP authorization, OAuth2, JWT, authorization server, resource server | [MCP Security](part5/13-mcp-security-oauth2-and-jwt.md) |
| elicitation | <span lang="ko">추가 정보 요청</span> | MCP Elicitation, user input request, @McpElicitation | [Human-in-the-Loop (HITL) Approval Gate](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md) |
| human-in-the-loop (HITL) | <span lang="ko">사용자 개입</span> | HITL, human approval, approval gate, approval workflow | [Human-in-the-Loop (HITL) Approval Gate](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md) |
| subagent | <span lang="ko">하위 에이전트</span> | sub-agent, agent delegation, TaskTool | [Enterprise Spring AI Agent CLI](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md) |
| multi-agent system | <span lang="ko">멀티 에이전트</span> | multi-agent orchestration, agent orchestration, agent as a tool, agents as tools | [Enterprise Spring AI Agent CLI](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md) |
| meta-tool | <span lang="ko">메타 툴</span> | TodoWrite, AskUserQuestion, Task, Skills, clarifying questions, task planning | [Enterprise Spring AI Agent CLI](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md) |
| dynamic tool discovery | <span lang="ko">동적 툴 탐색</span> | tool search, Tool Search Tool, progressive tool disclosure, ToolSearchToolCallingAdvisor | [Spring AI Agent Architecture, Implementation, and Dynamic Tool Discovery](part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md) |
| Agent Skills | <span lang="ko">에이전트 스킬</span> | skills, SKILL.md, skill files, SkillsTool | [Agent Skills](part6/19-agent-skills-extending-capabilities.md) |
| context engineering | <span lang="ko">컨텍스트 엔지니어링</span> | context management, context window, prompt design | [The Agent Loop and Context Engineering](part6/15-agent-loop-and-context-engineering.md) |
| ReAct | <span lang="ko">리액트</span> | reason and act, reasoning and acting, ReAct pattern, ReAct agent | [The Agent Loop and Context Engineering](part6/15-agent-loop-and-context-engineering.md) |
| workflow, autonomous agent | <span lang="ko">워크플로, 자율 에이전트</span> | agentic workflow, agentic systems, workflow patterns, Building Effective Agents | [The Agent Loop and Context Engineering](part6/15-agent-loop-and-context-engineering.md) |
| annotation | <span lang="ko">애너테이션</span> | @Tool, @McpTool, MCP annotations, Java annotations | [Building an MCP Server with Spring AI](part5/12-building-an-mcp-server-with-spring-ai.md) |
| Ollama | <span lang="ko">올라마</span> | local model, local LLM, self-hosted LLM, qwen3.5 | [ChatModel, ChatClient, and Prompt Engineering](part2/03-chatmodel-chatclient-and-prompt-engineering.md) |
| OpenAI | <span lang="ko">오픈AI</span> | OpenAI API, GPT | [Appendix: Switching to OpenAI, AI Evaluation, and Connecting External AI Agents](appendix/22-openai-evaluation-and-external-agents.md) |
| AI evaluation | <span lang="ko">AI 평가</span> | evals, model evaluation, evaluation testing, LLM-as-a-judge, Evaluator | [Appendix: Switching to OpenAI, AI Evaluation, and Connecting External AI Agents](appendix/22-openai-evaluation-and-external-agents.md) |
| Spring AI Playground | <span lang="ko">스프링 AI 플레이그라운드</span> | Playground, MCP Inspector, Tool Studio, Agentic Chat | [Appendix: Spring AI Playground](appendix/23-spring-ai-playground.md) |
