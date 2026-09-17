---
title: Home
description: Design enterprise AI agent systems with a 4-tier architecture and build them with Spring AI 2.0, following the order of the book's chapters.
hide:
  - navigation
  - toc
---

<div class="hero" markdown>

<p class="eyebrow">How to build enterprise AI agent systems</p>

# Design with a 4-tier architecture,<br>build with Spring AI

<p class="lead">Divide the system into four tiers: Channel, Orchestration, Capability, and Foundation. Then build what each tier needs with Spring AI 2.0. Following the book's chapters, you put together an agent system with domain-specific MCP servers, approval gates, subagents, and observability.</p>

<p class="cta" markdown="span">[Explore the 4-tier architecture](part6/14-four-tier-architecture-for-ai-agent-systems.md){ .md-button .md-button--primary } [Start with Chapter 1](part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md){ .md-button }</p>

<figure class="hero-figure" markdown>
![4-tier architecture for AI agent systems](assets/figures/fig6-23.png)
<figcaption>4-tier architecture for AI agent systems</figcaption>
</figure>

</div>

## At a glance: Spring AI technologies for each tier

<div class="grid cards" markdown>

-   <span class="tier-badge t1">T1</span> __Channel__

    ---

    Where users meet the agent: a terminal CLI, the web, or a native app. Because the channel is separate from the agent core, you can swap the channel alone. Every example in the book uses a CLI channel.

    Spring AI: `ChatClient` streaming responses, showing human-in-the-loop (HITL) requests

    [:octicons-arrow-right-24: ChatModel and ChatClient](part2/03-chatmodel-chatclient-and-prompt-engineering.md), [Chapter 2 chatbot CLI](part2/05-chat-memory-and-advisor-chain.md#hands-on-project-for-this-chapter)

-   <span class="tier-badge t2">T2</span> __Orchestration__

    ---

    The main agent. It reasons, plans, picks tools, and controls the loop. The competitive edge is not the model but this tier: planning, control, and state management.

    Spring AI: `ChatClient`, the advisor chain, `ToolCallingAdvisor`, `MessageChatMemoryAdvisor`, `ToolSearchToolCallingAdvisor`

    [:octicons-arrow-right-24: Recursive Advisors and Tool Loop Control](part6/16-recursive-advisor-and-tool-loop-control.md)

-   <span class="tier-badge t3">T3</span> __Capability__

    ---

    The external capabilities an agent uses: tools, MCP, search, and memory. A local method, a subagent in the same JVM, and a remote server connected over MCP all look like the same kind of tool to the main agent.

    Spring AI: `@Tool`, MCP clients and servers, `TaskTool` subagents, skills, RAG

    [:octicons-arrow-right-24: Designing tool calling](part4/09-tool-calling-design-in-spring-ai.md), [MCP basics](part5/11-mcp-basics-and-spring-ai-mcp-client.md)

-   <span class="tier-badge t4">T4</span> __Foundation__

    ---

    The underlying resources that the two intelligence tiers use: models, data, and infrastructure. Models belong here too, because they are a resource that supplies reasoning.

    Spring AI: `ChatModel`, `EmbeddingModel`, `VectorStore`, the ETL pipeline

    [:octicons-arrow-right-24: RAG architecture and ETL](part3/06-rag-architecture-and-etl-pipeline.md), [Embeddings and vector stores](part3/07-embedding-models-and-vector-stores.md)

-   <span class="tier-badge tx">Cross</span> __Cross-cutting concerns__

    ---

    Concerns shared by every tier rather than owned by one: observability, governance and human-in-the-loop (HITL), security and sandboxing, audit logging, and cost and rate control.

    Spring AI: Micrometer observations and OTLP, MCP Elicitation approval gates, MCP Security (OAuth2, JWT)

    [:octicons-arrow-right-24: MCP security](part5/13-mcp-security-oauth2-and-jwt.md), [Approval gates](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)

-   :material-layers-triple:{ .lg .middle } __Standard interface: MCP__

    ---

    The boundary where Orchestration calls Capability, implemented today with the MCP protocol. Where you place this boundary decides how clients and servers, processes, teams, and deployment units are divided.

    [:octicons-arrow-right-24: Deploying the 4 tiers across clients and servers](part6/14-four-tier-architecture-for-ai-agent-systems.md)

</div>

## Splitting the deployment across clients and servers

The four tiers are a logical division, so how you deploy them can differ from system to system. The book makes the CLI that users run in a terminal the main agent and splits business capabilities into domain-specific MCP servers.

<figure class="wide-figure" markdown>
![Spring AI agent CLI system design that deploys the 4-tier architecture across a client and servers](assets/figures/fig6-24.png)
<figcaption>Deploying the 4-tier architecture across a client and servers</figcaption>
</figure>

The client CLI process holds the Channel (T1), Orchestration (T2), and the local capabilities that run directly (T3). Capabilities operated separately, such as inventory lookup or internal document search, move to MCP servers, and the two sides connect over MCP on Streamable HTTP. When a new business capability is needed, you can leave the main agent's code as it is, add an MCP server, and register only its connection details.

<figure class="wide-figure tall" markdown>
![Components of the Spring AI agent CLI system](assets/figures/fig6-27.png)
<figcaption>The agent CLI system mapped to Spring AI components</figcaption>
</figure>

Mapped to Spring AI components, the design looks like the figure above. `ChatClient` and the advisor chain handle orchestration, and local `@Tool` methods, community tools, and tools on remote MCP servers all come together behind a single tool interface. In Chapter 6, the [enterprise agent CLI article](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md) completes this setup in code, and the [observability article](part6/21-observability-for-ai-agents-micrometer-otel-otlp.md) turns on tracing across every tier. The articles before them cover the parts in this figure one at a time.

## Reading along with the book's chapters

These articles follow the Korean book *The Complete Guide to AI Agent Development with Spring AI 2.0* chapter by chapter. Each article covers the chapter's key concepts and explanations, along with code from the [example repository](https://github.com/JM-Lab/spring-ai-agent-book). For readers who want to dig into design rationale, internals, and measured results, each article lists the matching sections of the book at the end. If you find a mistake, please report it by following the [error reporting](book.md#reporting-errors) guide.

The example code is shared with the Korean edition, so prompts, comments, and console output inside code blocks stay in Korean, as do the figures taken from the book; the articles explain what each example does.

<!-- chapters:start -->
<div class="grid cards" markdown>

-   <span class="chapter-no">Chapter 1</span> __AI Agents: The Start of a New Paradigm__

    ---

    1. [From LLM Calls to AI Agents: Why Spring AI for Java Developers](part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md)
    2. [From Spring AI 1.0 to 2.0: What Changed](part1/02-whats-new-in-spring-ai-2-0.md)

-   <span class="chapter-no">Chapter 2</span> __The Spring AI Framework__

    ---

    1. [ChatModel, ChatClient, and Prompt Engineering](part2/03-chatmodel-chatclient-and-prompt-engineering.md)
    2. [Tokens and Structured Output: Getting Model Answers as Java Objects](part2/04-tokens-and-structured-output.md)
    3. [Chat Memory and the Advisor Chain: The Core of Spring AI](part2/05-chat-memory-and-advisor-chain.md)

-   <span class="chapter-no">Chapter 3</span> __Spring AI and RAG__

    ---

    1. [RAG Architecture and the ETL Pipeline: Turning Documents into Knowledge](part3/06-rag-architecture-and-etl-pipeline.md)
    2. [Embedding Models and Vector Databases](part3/07-embedding-models-and-vector-stores.md)
    3. [From Naive RAG to Modular RAG: The Spring AI RAG Framework](part3/08-from-naive-rag-to-modular-rag.md)

-   <span class="chapter-no">Chapter 4</span> __Tool Calling__

    ---

    1. [Designing Tool Calling: How LLMs Connect to the Real World](part4/09-tool-calling-design-in-spring-ai.md)
    2. [Implementing Tools and Controlling Execution: From @Tool to ToolCallingManager](part4/10-tool-implementation-and-execution-control.md)

-   <span class="chapter-no">Chapter 5</span> __Spring AI MCP__

    ---

    1. [MCP Basics and the Spring AI MCP Client](part5/11-mcp-basics-and-spring-ai-mcp-client.md)
    2. [Building an MCP Server with Spring AI: From Boot Starters to Annotations](part5/12-building-an-mcp-server-with-spring-ai.md)
    3. [MCP Security: Protecting MCP Clients and Servers with OAuth2 and JWT](part5/13-mcp-security-oauth2-and-jwt.md)

-   <span class="chapter-no">Chapter 6</span> __AI Agents__

    ---

    1. [4-Tier Architecture for AI Agent Systems](part6/14-four-tier-architecture-for-ai-agent-systems.md)
    2. [The Agent Loop and Context Engineering](part6/15-agent-loop-and-context-engineering.md)
    3. [Recursive Advisors and Tool Loop Control](part6/16-recursive-advisor-and-tool-loop-control.md)
    4. [Spring AI Agent Architecture, Implementation, and Dynamic Tool Discovery](part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md)
    5. [Human-in-the-Loop (HITL): An Approval Gate with MCP Elicitation](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md)
    6. [Agent Skills: Extending Agent Capabilities with Reusable Skills](part6/19-agent-skills-extending-capabilities.md)
    7. [An Enterprise Spring AI Agent CLI: Multi-Agent and Meta-Tool Orchestration](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md)
    8. [Observability for AI Agents: Micrometer, OTel GenAI Semantic Conventions, and OTLP](part6/21-observability-for-ai-agents-micrometer-otel-otlp.md)

-   <span class="chapter-no">Appendix</span> __Taking the hands-on work further__

    ---

    1. [Switching to OpenAI, AI Evaluation, and Connecting External AI Agents](appendix/22-openai-evaluation-and-external-agents.md)
    2. [Spring AI Playground](appendix/23-spring-ai-playground.md)

</div>
<!-- chapters:end -->

## The book, the example repository, and the Playground

<div class="book-card" markdown>
<div class="cover-wrap"><img class="off-glb" src="../assets/img/cover.jpg" alt="Cover of the Korean book The Complete Guide to AI Agent Development with Spring AI 2.0"></div>
<div markdown>
**The Complete Guide to AI Agent Development with Spring AI 2.0**
<br>Korean title: <span lang="ko">스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드</span>
<br>By Jemin Huh, Wikibooks, published September 17, 2026, 692 pages, in Korean

The source of this site. From `ChatClient` to RAG, tool calling, MCP, and enterprise AI agents built on the 4-tier architecture, every exercise runs on Spring AI 2.0 GA with local models only. This site covers the essentials, and the book goes further into the design rationale, internals, and measured results of each section.

[Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary } [About the book](book.md){ .md-button }
</div>
</div>

<div class="grid cards" markdown>

-   :material-github:{ .lg .middle } __Example repository__

    ---

    An independent Maven project for each chapter, using Java 21, Spring Boot 4, Spring AI 2.0.0 GA, and Ollama with `qwen3.5:4b` and `bge-m3`. Code blocks on this site come straight from the repository files.

    [:octicons-arrow-right-24: JM-Lab/spring-ai-agent-book](https://github.com/JM-Lab/spring-ai-agent-book)

-   :material-flask:{ .lg .middle } __Spring AI Playground__

    ---

    A separate open source project, with the author as lead maintainer. It is an incubating project of the Spring AI Community: a desktop application built with Spring AI, with a UI developed in Vaadin. It lets you try five areas in one place: Tool Studio, MCP server connections and the MCP Inspector, Vector Database and RAG, Agentic Chat, and Observability. You can connect the book's MCP servers to it as they are (Appendix D of the book).

    [:octicons-arrow-right-24: About the Playground](appendix/23-spring-ai-playground.md), [Download](https://spring-ai-community.github.io/spring-ai-playground), [GitHub](https://github.com/spring-ai-community/spring-ai-playground)

</div>
