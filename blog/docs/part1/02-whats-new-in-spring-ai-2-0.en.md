---
title: "From Spring AI 1.0 to 2.0: What Changed"
description: "Spring AI grew from 1.0, built for AI integration, to 2.0, built for agents. The major changes: platform, configuration, memory, tool calling, MCP, agents."
tags:
  - Chapter 1
---

# From Spring AI 1.0 to 2.0: What Changed

<div class="post-meta" markdown>
<span class="tier-chip all">All 4 tiers</span> Book section 1.3.3 | Examples [`basic-chat`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/basic-chat), [`chapter2`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter2), [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

The [previous article](../part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md) looked at why Java developers need Spring AI. This article summarizes what changed as Spring AI went from 1.0 to 2.0. It regroups Table 1.1 from Section 1.3.3 of the book by area, checks how each change shows up in the code of the example repository, and then points you to the articles on this site that cover it in detail.

The book's explanations, examples, and hands-on projects are all based on Spring AI 2.0 and run on Ollama models, which are easy to start locally. The code blocks on this site also come from the book's example repository.

## 1.0 for AI integration, 2.0 for agents

The goals Spring AI set out became real APIs in the 1.0 GA release in May 2025. With a single `ChatClient` for working with multiple model providers, a tool calling abstraction, chat memory based on `ChatMemory`, and RAG advisors, it established itself as the leading AI development framework in the Java world. It also started supporting MCP early, and the code written at that time became the foundation of the official MCP Java SDK.

There were shortcomings, too. The loop that calls a tool, feeds in the result, and calls the model again was hidden inside the model implementation. The fine-grained control that agents need, such as limiting the number of iterations or getting approval before execution, had to be built separately by developers.

The 2.0 GA release in June 2026 reworked this foundation for the age of agents. Rather than adding a few features, it is an architectural change: the platform base moved up a generation, and the tool loop was pulled out into a component of the advisor chain.

## The major changes at a glance

Condensed by area, Table 1.1 of the book looks like this.

| Area | Spring AI 1.0 | Spring AI 2.0 |
| --- | --- | --- |
| Base platform | Spring Boot 3.x, Jackson 2, Java 17 or later | Spring Boot 4.x (required) and Spring Framework 7, Jackson 3, Java 17 or later with 21 recommended |
| Model providers | 20 providers supported through a single `ChatClient` | The core focuses on 7 major providers, including OpenAI, Anthropic, and Ollama |
| Options and configuration | Options modified with setters, complex configuration key structure | Options are immutable and changed with `mutate()`, configuration keys flattened |
| Chat memory | Prompt-based and Message-based approaches coexist | Consolidated into `MessageChatMemoryAdvisor`, conversation ID specified at call time |
| Tool calling | Execution controlled with flags, tools referenced by bean name strings | Tools registered as `ToolCallback` objects, loop controlled by the recursive advisor `ToolCallingAdvisor` |
| MCP | Early support scattered across several places | Officially part of core, declared with annotations, integrated with OAuth-based security |
| Agent support | Basic building blocks provided | Hooks for tool loop control and dynamic tool discovery added |
| Platform support lifetime | OSS support for Spring Boot 3.x ended in June 2026 | Spring Boot 4.x is the generation currently under OSS support, with a minor release every 6 months |

## Base platform and model providers

The first change that stands out is the platform base. Spring AI 2.0 runs on Spring Boot 4.x, Spring Framework 7, and Jackson 3. Java 17 or later is enough, but 21 is recommended, and Chapter 2 of the book explains that 2.0 makes active use of the latest features of Java 21 and later. You can check this baseline in each chapter's `pom.xml` in the example repository.

```xml title="pom.xml"
--8<-- "chapter2/pom.xml:7:12"
--8<-- "chapter2/pom.xml:20:23"
--8<-- "chapter2/pom.xml:38:48"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/pom.xml)</span>

The parent POM is Spring Boot 4.0.7, the Java version is 21, and `spring-ai.version` is 2.0.0. Because `dependencyManagement` imports `spring-ai-bom`, starters such as `spring-ai-starter-model-ollama` do not specify a version. All three values are the same in every project from `basic-chat` to `chapter6`.

On the model provider side, the direction changed. Where 1.0 supported 20 providers through a single `ChatClient`, the 2.0 core focuses on 7 major providers, including OpenAI, Anthropic, and Ollama. Application code relies on common interfaces such as `ChatModel` and `ChatClient`, so even when you switch providers from Ollama to OpenAI, you mostly touch only the starter and the configuration. The [ChatModel and ChatClient article](../part2/03-chatmodel-chatclient-and-prompt-engineering.md) covers the design of the common interfaces, and the [appendix article](../appendix/22-openai-evaluation-and-external-agents.md) covers how to run the examples with OpenAI.

## Options and configuration

In 1.0, you modified option objects with setters, and the key structure of configuration files was complex. In 2.0, option objects are immutable. To change a value, you call `mutate()` to get a builder holding the existing values, change only the values you need, and build a new options object. The configuration keys were also reorganized into a flat structure that is easy to read.

```yaml title="application.yml (chapter2)"
--8<-- "chapter2/src/main/resources/application.yml:4:15"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/src/main/resources/application.yml)</span>

You choose the chat model provider with `spring.ai.model.chat` and put the Ollama server address in `spring.ai.ollama.base-url`. The model name (`model`) and whether to use reasoning (`think`) sit side by side directly under `spring.ai.ollama.chat`. Running with OpenAI follows the same shape: you change `spring.ai.model.chat` to `openai` and fill in the API key and model name.

You can see immutable options in the Chapter 6 code. When [`OrchestrationToolCallingAdvisor`](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/OrchestrationToolCallingAdvisor.java#L139-L145) adds meta-tools to the request in each round, it does not modify the existing options directly. It gets a builder with `mutate()`, builds new options with only the tool list changed, and creates a new request object the same way. The [ChatModel and ChatClient article](../part2/03-chatmodel-chatclient-and-prompt-engineering.md) continues with how `ChatOptions` is used.

## Chat memory

Spring AI 1.0 had both Prompt-based and Message-based memory advisors. Version 2.0 consolidated them into `MessageChatMemoryAdvisor`, which puts the stored conversation into the prompt directly as a list of message objects, and changed the conversation ID so that it is passed at the moment of the call. Because each request now makes it clear which conversation's history to read and write, you can reduce accidents in which different users' conversations get mixed up. Checking which user may use which conversation ID is up to the application.

```java title="Ch2Step4_Memory.java"
--8<-- "chapter2/src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java:31:67"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter2/src/main/java/kr/jmlab/spring/ai/agent/book/chapter2/Ch2Step4_Memory.java)</span>

The constructor creates a `MessageWindowChatMemory` that keeps the 20 most recent messages and registers a `MessageChatMemoryAdvisor` that uses it. This advisor is registered only once, as a default advisor. For the conversation ID, the code keeps using one UUID created at startup, and each time a question is sent, `advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))` indicates which conversation this request belongs to. Registering advisors at build time and changing only the parameters per request is also the arrangement that Section 2.8.1 of the book recommends. The [chat memory and advisor chain article](../part2/05-chat-memory-and-advisor-chain.md) looks at chat memory repositories and the execution order of advisors in detail.

## Tool calling: a tool loop controlled by an advisor

This is the area the book singles out as the most important change. In 1.0, you controlled tool execution with flags and referred to tools by bean name strings. In Table 1.1, the book sums up 2.0 as having moved to registering tools as `ToolCallback` objects.

What runs the loop has changed as well. The loop is now run by `ToolCallingAdvisor` inside the advisor chain. This component, a recursive advisor, repeats tool execution, result injection, and calling the model again until a final answer comes out. The loop that used to be buried inside the model implementation is now a part you can combine with other advisors, which makes it easier to attach control logic such as iteration limits or user approval gates. In 2.0, the tool loop runs automatically even if you do not register it, but if you want to decide its position in the chain and its behavior, you configure it yourself.

```java title="AgentConfig.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java:174:192"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/AgentConfig.java)</span>

The code above is the factory that the Chapter 6 main agent uses to create a loop advisor for each request. The default path adds two settings to `ToolCallingAdvisor.builder()`. `toolExecutionEligibilityChecker` gets a safety guard that stops the loop once the tool execution rounds in a single request exceed the limit (`MAX_TOOL_ROUNDS`, 10), and `advisorOrder` fixes the advisor's position in the chain. When there are many tools and dynamic tool discovery is turned on, the same settings are applied to the `ToolSearchToolCallingAdvisor` builder. The advisor is created anew for each request so that its round counter is not shared with other requests. The [tool calling design article](../part4/09-tool-calling-design-in-spring-ai.md) and the [tool implementation and execution control article](../part4/10-tool-implementation-and-execution-control.md) cover how to define tools, and the [recursive advisor article](../part6/16-recursive-advisor-and-tool-loop-control.md) covers the flow of the recursive loop.

## MCP and agent support

Table 1.1 of the book sums up MCP support as scattered across several places in the 1.0 days and officially settled in core in 2.0. Server features are declared with annotations. `OperationsMcpTools` in the Chapter 6 operations server exposes tools for checking stock and placing purchase orders through methods annotated with `@McpTool`. Security is handled by the Spring AI Community's mcp-security project. If you use this project to configure an MCP server as an OAuth2 resource server, the server itself validates the JWT in the request and runs tools only after doing so. The [MCP basics article](../part5/11-mcp-basics-and-spring-ai-mcp-client.md) covers MCP clients, the [building an MCP server article](../part5/12-building-an-mcp-server-with-spring-ai.md) covers servers, and the [MCP security article](../part5/13-mcp-security-oauth2-and-jwt.md) covers security.

Support for agents has also broadened. Where 1.0 provided the basic building blocks for agents, 2.0 added hooks for tool loop control and dynamic tool discovery. `ToolCallingAdvisor` provides hook methods that let you step in right before the loop starts, before and after the downstream chain is called in each iteration, and right after the loop ends. Developers override these hooks to put safeguards in place, such as iteration limits or cost limits. The `OrchestrationToolCallingAdvisor` you saw earlier also overrides the `doBeforeCall` and `doBeforeStream` hooks to expose meta-tools in every round. `ToolSearchToolCallingAdvisor`, which appeared in the tool loop code, handles dynamic tool discovery. Instead of sending every tool definition to the model each time, it shows the model a single tool for finding tools and adds to the context only the definitions of the tools the model searched for. The [recursive advisor article](../part6/16-recursive-advisor-and-tool-loop-control.md) covers the hooks, the [agent architecture and dynamic tool discovery article](../part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md) covers dynamic tool discovery, and the [human-in-the-loop (HITL) article](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md) covers how to get approval before risky operations.

## Where this fits in the 4-tier architecture

The changes in 2.0 are not confined to one tier. `ToolCallingAdvisor` and `MessageChatMemoryAdvisor` form the backbone of T2 Orchestration, which manages the loop and state, while `ToolCallback` and MCP support, now in core, unify the way T2 calls T3 capabilities. Model provider support reaches the model resources in T4 Foundation, and the OAuth2 configuration of mcp-security belongs to security, a cross-cutting concern. From the next article on, you build these parts one at a time, following Chapter 2 of the book. First up is [ChatModel, ChatClient, and Prompt Engineering](../part2/03-chatmodel-chatclient-and-prompt-engineering.md).

## More in the book

!!! book "Book section 1.3.3"
    - **1.3.3 The evolution of Spring AI:** How 1.0 became the leading AI framework in the Java world, and the direction in which 2.0 changed its design for the age of agents
    - **Table 1.1 Major changes in Spring AI 1.0 and 2.0:** The original area-by-area comparison that the table in this article condenses
    - **Details by chapter:** Options and configuration in Section 2.4, chat memory and advisors in Sections 2.7-2.8, tool execution control in Section 4.4, MCP servers and security in Sections 5.3-5.4, recursive advisors in Section 6.3, and dynamic tool discovery in Section 6.4.5

    [About the book](../book.md){ .md-button } [Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## References

- [Spring AI 2.0.0 GA Available Now](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now): the announcement of the Spring AI 2.0 GA release
