---
title: "4-Tier Architecture for AI Agent Systems"
description: "The 4-tier architecture behind agent services like Claude Code and Codex: Channel, Orchestration, Capability, Foundation, and cross-cutting concerns."
tags:
  - Chapter 6
---

# 4-Tier Architecture for AI Agent Systems

<div class="post-meta" markdown>
<span class="tier-chip all">All 4 tiers</span> Book sections 6.4.1-6.4.2, 6.6.1-6.6.2 | Example [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

If you have worked through `ChatClient` and the advisor chain in Chapter 2, RAG in Chapter 3, tool calling in Chapter 4, and MCP in Chapter 5, you already have most of the parts for building an AI agent. What remains is assembly. For a system that several teams will share and run for a long time, it helps to decide on its tiers before writing any code.

This article starts from the structure that major AI agent services share. It then looks at the 4-tier architecture the book proposes, a design that deploys it across a client and servers, and the code structure of the Chapter 6 example. Every article on this site notes where its technology fits in the 4 tiers, and this article is the map they refer to.

## The shared structure of major AI agent services

The book analyzes two commercial services, Claude Code and Codex, and two open source projects, OpenCode and OpenClaw, from three angles: where the loop runs, how capabilities are connected, and how the system extends outward.

- **Claude Code**: Everything is a tool, from file editing and shell execution to task planning (TodoWrite), asking the user (AskUserQuestion), skills, and delegating to subagents (Task).
- **Codex**: The CLI, the IDE extension, and the web version share the same agent loop implementation, and features beyond the built-in tools are connected through MCP.
- **OpenCode**: An open source coding agent that supports more than 75 LLM providers, with a tool set that largely overlaps with Claude Code's.
- **OpenClaw**: A personal AI assistant that you hand tasks to through messaging apps. It embeds the runtime of the coding agent Pi, so it runs on the same loop and tool structure.

Their domains and distribution models differ, but they share three structural elements: a single ReAct-style agent loop, a single tool interface that every action passes through, and MCP for external extension.

<figure class="wide-figure" markdown>
![Common architecture of major AI agent systems: a single loop and a tool execution area](../assets/figures/fig6-14.png)
<figcaption>Common architecture of major AI agent systems: a single loop and a tool execution area</figcaption>
</figure>

The book traces why this structure keeps recurring across products to the open-closed principle. The loop code stays closed while the tool list stays open, so a new capability means adding a tool without touching the loop. Because every action passes through one gateway, operational concerns such as permission control and user approval can be handled right there. The approval modes and sandbox in Codex are examples.

In Spring AI, `@Tool` and `ToolCallback` provide the tool interface, MCP handles external extension, and `ToolCallingAdvisor` controls the tool loop. Agent-specific tools for planning, asking questions, skills, and delegation come from the Spring AI Community. The [Spring AI agent architecture and implementation](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md) article covers the implementation in detail.

<figure class="wide-figure tall" markdown>
![Spring AI agent architecture](../assets/figures/fig6-15.png)
<figcaption>Spring AI agent architecture</figcaption>
</figure>

## Multi-agent systems and standards for connecting agents

As a system grows to dozens of tools and the contexts of unrelated tasks mix in a single conversation, the model becomes more likely to pick the wrong tool or give weaker answers. That is when a multi-agent structure with separate roles becomes necessary. Major products and frameworks do not build new infrastructure for this. They use the agent as a tool pattern, in which one agent calls another agent as a tool. Examples include `agent.as_tool()` in the OpenAI Agents SDK, `AgentTool` in Google ADK, and `TaskTool` from the Spring AI Community. The caller keeps control, and the subagent works in an isolated context and returns only a summary of its results. Since this adds one more tool, one that contains an LLM, to the tool execution area, you can see it as an extension of the single-agent structure.

Once tool calls cross process and team boundaries, you need a communication standard. The two main candidates are MCP and A2A (Agent2Agent). After Anthropic released MCP, OpenAI, Google, and Microsoft adopted it in turn, and in December 2025 it moved to the Agentic AI Foundation under the Linux Foundation. The standard also includes Elicitation, which asks the user for confirmation during tool execution. A2A is a protocol that Google proposed specifically for collaboration between agents. Version 1.0 came out in March 2026, but its Spring AI integration is still in community incubation. For that reason, the book connects agents as tools through MCP, which Spring AI core officially supports.

## The 4-tier architecture

The book draws on existing references that divide agent systems into layers. CoALA, a cognitive architecture for language agents, structures an agent into modules for memory, action space, and decision-making procedures. Bain & Company named orchestration, observability, and data access (governance) as the core layers of an agentic AI platform, and Microsoft published a multi-agent reference architecture divided into orchestration, agent, tool, and observability layers. The 4-tier architecture builds on the layer separation that keeps appearing in these references and makes it concrete, so that each tier maps one-to-one to Spring AI components. The book designs an AI agent system as four tiers plus cross-cutting concerns, as shown in the following figure.

<figure class="wide-figure" markdown>
![4-tier architecture for AI agent systems](../assets/figures/fig6-23.png)
<figcaption>4-tier architecture for AI agent systems</figcaption>
</figure>

At the center are the two intelligence tiers. Orchestration (T2), which directs the work, and Capability (T3), which carries out the assigned work, face each other across a standard interface.

### T1 Channel

The point where users and the agent meet. It takes input, streams responses, and handles human involvement such as approvals and clarifying questions. In the book's project, a terminal CLI fills this role. Because the channel is separate from the agent core, you can keep the core as it is and switch only the channel to a web or mobile app.

### T2 Orchestration

The tier that directs the work: it decides what to do, when, and in what order. The decisions come from the LLM's reasoning loop, not from deterministic code written by developers. The loop breaks down goals, chooses which capabilities to call, combines the results, and keeps control until the end. As models quickly converge at a high level of performance, the book sees this tier, where planning, control, and state management are designed, as the place where a product's competitive edge is decided.

In Spring AI, this tier centers on `ChatClient` and the advisor chain. The key pieces are `ToolCallingAdvisor`, which runs the tool loop, `ToolSearchToolCallingAdvisor`, which finds and attaches the tools that are needed, and `MessageChatMemoryAdvisor`, which remembers the conversation. The book also places meta-tools such as task planning (TodoWrite) and clarifying questions (AskUserQuestion) in this tier.

### The standard interface between the two intelligence tiers

T2 and T3 meet at a standard interface for calling capabilities. If you keep the specification of what is exchanged separate from the protocol that carries it, you can later replace today's MCP with another protocol and still leave the code of both intelligence tiers unchanged.

### T3 Capability

The tier that actually carries out the work T2 assigns. Here, a capability means a set of tools that the agent uses. Local `@Tool` methods, features of MCP servers, `TaskTool` subagents, and search and memory are all provided through a single tool interface. Because a capability can contain another agent, T3 can also reason on its own. Capabilities are encapsulated behind the standard interface, so adding a new tool or MCP server requires no changes to T2.

### T4 Foundation

The underlying resources that T2 and T3 use. This tier includes models (`ChatModel`), the vector databases and relational databases that hold knowledge and business data, and internal and external APIs. Models sit here rather than in an intelligence tier because they, too, are a resource that supplies reasoning.

### Cross-cutting concerns that span every tier

Cross-cutting concerns do not belong to any single tier. They operate across every tier, from a request arriving at the channel to the model call.

- **Observability**: Collects traces, metrics, and token usage as standard telemetry and exports them.
- **Governance and human-in-the-loop**: Applies tool permissions and policies, and routes risky operations through an approval gate (`@McpElicitation`).
- **Security and sandboxing**: Handles authentication and authorization, and isolates the tool execution environment.
- **Audit logging**: Records tool calls, agent decisions, and user approvals.
- **Cost and rate control**: Sets limits on tokens and API calls and tracks costs.

The four tiers are a logical division. In a real deployment, the channel and orchestration may share a single CLI process, so the physical layout is designed separately.

## Deploying across clients and servers

The book's final project is a multi-agent system in which a CLI installed by the user acts as the main agent and calls business capabilities from across the company through MCP servers. The client is the head that runs the loop and decides what to call. The servers are the hands and feet: each team operates its own, and they provide business capabilities that many parts of the company share.

### The tool boundary is the system boundary

To the main agent, a local method, a subagent in the same JVM, and a remote server connected over MCP are all the same kind of tool. So where you draw the line within T3 is what divides the client from the servers. Capabilities that run directly in the same process stay in the client as local tools, and capabilities that are operated separately are split out into MCP servers.

This line also matches boundaries outside the code. The client and servers run as separate processes, each server can have its own team and implementation language, and you can scale out only the servers under heavy load. Every call that crosses the boundary creates a trace span, which makes execution across multiple processes easier to follow. When a new department's capability is needed, you can leave the main agent as it is, deploy an MCP server, and add only its connection details. In effect, the agent core stays closed while capabilities stay open through MCP.

<figure class="wide-figure" markdown>
![Spring AI agent CLI system design that deploys the 4 tiers across a client and servers](../assets/figures/fig6-24.png)
<figcaption>Spring AI agent CLI system design that deploys the 4 tiers across a client and servers</figcaption>
</figure>

In the figure, the MCP boundary cuts across T3. T1, T2, and the local part of T3 live in one CLI process, and the remote part of T3 is split into domain-specific MCP servers. In the example, two servers provide the remote capabilities. The knowledge server (8086) exposes a RAG subagent as the `rag_answer_question` tool. The operations server (8085) provides tools for checking stock (`check_stock`) and placing purchase orders (`place_purchase_order`), and a purchase order goes through an `@McpElicitation` approval gate first. The client and the servers each connect to T4 resources on their own, and observability spans both sides.

Subagents also differ in how isolated they are, depending on where they run. A subagent that the client delegates to through `TaskTool` isolates only its context within the same process, while the subagent in the knowledge server runs in a separate process and is deployed and scaled separately. Either way, the main agent keeps control.

## The 4 tiers in code

In the Chapter 6 example, packages correspond directly to tiers. `channel` maps to T1, `orchestration` to T2, and `capability` to T3. Following the tool boundary principle, T3 is further divided into `local`, which runs in the same process, and `remote`, which is split out into MCP servers. `resources` holds the configuration for connecting to T4 resources and the RAG documents.

```text
chapter6/src/main/
├── java/kr/jmlab/spring/ai/agent/book/chapter6/
│   ├── Chapter6Application.java    # Runs the client and the two servers by profile
│   ├── channel/                    # T1 Channel: step-by-step CLI runners
│   │   ├── Ch6Step1_SpringAIAgent.java ... Ch6Step6_Orchestration.java
│   │   ├── Ch6EnhancedSpringAIAgentCli.java  # Final integrated CLI
│   │   └── ConsoleElicitationHandler.java    # Console approval handler
│   ├── orchestration/              # T2 Orchestration
│   │   ├── SpringAIAgent.java      # Main agent
│   │   ├── AgentConfig.java        # Beans for the core and enhanced agents
│   │   ├── AgentSafety.java        # Loop safety guard
│   │   ├── OrchestrationToolCallingAdvisor.java
│   │   └── ThinkTraceAdvisor.java, ToolCallTraceAdvisor.java,
│   │       ToolLoopMetricsAdvisor.java
│   └── capability/                 # T3 Capability
│       ├── ToolNames.java
│       ├── local/                  # Local capabilities that run in the same process
│       │   ├── DateTimeTools.java, CalculatorTools.java,
│       │   │   InventoryTools.java
│       │   └── ChainWorkflow.java
│       └── remote/                 # Remote capabilities split out into MCP servers
│           ├── Ch6OpsMcpServer.java, OperationsMcpTools.java
│           └── Ch6KnowledgeMcpServer.java, KnowledgeMcpTools.java,
│               KnowledgeServerConfig.java, KnowledgeBase.java,
│               RagDocumentLoader.java, RagAnswerService.java
└── resources/                      # Configuration and data
    ├── application.yml             # Client (main agent)
    ├── application-ops.yml         # Operations MCP server (8085)
    ├── application-knowledge.yml   # Knowledge MCP server (8086)
    ├── agents/report-writer.md     # Declarative subagent definition
    ├── skills/restock-policy/SKILL.md
    └── data/                       # RAG documents read by the knowledge server
```

The client and the two servers live in one module but start as separate processes by profile, so they share a repository while remaining separate deployment units.

```java title="SpringAIAgent.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java:19:52"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java)</span>

There is a single main agent class, `SpringAIAgent`. Its constructor builds a `ChatClient` from a system prompt, tools, and advisors, and `run()` streams the response with a new tool loop advisor attached for each request. Because tools arrive as a `List<ToolCallback>`, a local tool and a tool from an MCP server are the same type to T2. The core agent and the enhanced agent are likewise two beans of the same class, built with different prompts, tools, and loop advisors.

```java title="AgentConfig.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java:93:98"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java)</span>

The core agent bean in `AgentConfig` appends the MCP servers' tool callbacks to the set of local `@Tool` methods and passes the result to `SpringAIAgent`. In this code, the boundary between the client and the servers shows up only as a list concatenation.

```yaml title="application.yml"
--8<-- "chapter6/src/main/resources/application.yml:38:47"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/resources/application.yml)</span>

A remote server is connected by listing its name and address under `streamable-http.connections`, and `toolcallback.enabled` exposes that server's tools to the main agent. Just below this part of the file, the connection to the knowledge server (8086) is commented out, so uncommenting it attaches one more server without changing any Java code.

## Where this fits in the 4-tier architecture

This article is a map of all four tiers rather than any single one. The models and vector databases of T4 were covered in Chapters 2 and 3, and the tools and MCP of T3 in Chapters 4 and 5. The remaining Chapter 6 articles build the agent on top of this map.

- [The Agent Loop and Context Engineering](../part6/15-agent-loop-and-context-engineering.md): the loop and context of T2
- [Recursive Advisors and Tool Loop Control](../part6/16-recursive-advisor-and-tool-loop-control.md): tool loop control in T2
- [Spring AI Agent Architecture and Implementation](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md): the agent implementation that connects T2 and T3, and dynamic tool discovery
- [Human-in-the-Loop (HITL) Approval Gate](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md): governance as a cross-cutting concern
- [Agent Skills](../part6/19-agent-skills-extending-capabilities.md): extending T3 capabilities
- [Enterprise Spring AI Agent CLI](../part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md): assembling every tier in code
- [Observability for AI Agents](../part6/21-observability-for-ai-agents-micrometer-otel-otlp.md): observability as a cross-cutting concern

The next article, [The Agent Loop and Context Engineering](../part6/15-agent-loop-and-context-engineering.md), starts with the agent loop that underpins T2.

## More in the book

!!! book "Book sections 6.4.1-6.4.2, 6.6.1-6.6.2"
    - How the four products define their loops and extend them, compared side by side
    - Agent-as-a-tool patterns and handoff approaches in each framework
    - The state of standardization for MCP and A2A, compared
    - Physical deployment of the 4 tiers and a table mapping them to Spring AI components
    - Ways to isolate subagents and to extend to a web channel
    - Standard telemetry and a step-by-step implementation roadmap

    [About the book](../book.md){ .md-button } [Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## References

- [Building agents with the Claude Agent SDK](https://claude.com/blog/building-agents-with-the-claude-agent-sdk): Anthropic's explanation of the agent loop
- [Unrolling the Codex agent loop](https://openai.com/index/unrolling-the-codex-agent-loop/): a walkthrough of the Codex agent loop
- [OpenCode Docs](https://opencode.ai/docs): official OpenCode documentation
- [OpenClaw Docs](https://docs.openclaw.ai): official OpenClaw documentation
- [OpenAI Agents SDK: Agents as tools](https://openai.github.io/openai-agents-python/tools/): the pattern of calling agents as tools
- [Google ADK: Agent-as-a-Tool](https://google.github.io/adk-docs/tools-custom/function-tools/#agent-tool): agent tools in ADK
- [CoALA (arXiv:2309.02427)](https://arxiv.org/abs/2309.02427): the paper on cognitive architectures for language agents
- [Multi-agent Reference Architecture](https://microsoft.github.io/multi-agent-reference-architecture/): Microsoft's reference architecture
