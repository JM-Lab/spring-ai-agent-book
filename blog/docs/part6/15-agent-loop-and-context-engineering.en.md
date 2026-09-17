---
title: "The Agent Loop and Context Engineering"
description: "Workflow patterns versus autonomous agents, how Spring AI supports autonomous agents, the move from prompt to context engineering, and ReAct."
tags:
  - Chapter 6
---

# The Agent Loop and Context Engineering

<div class="post-meta" markdown>
<span class="tier-chip t2">T2 Orchestration</span> Book sections 6.1-6.2 | Example [`chapter6`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter6)
</div>

An AI agent does not stop after calling the model once. The model looks at the situation, decides on the next action, carries it out with a tool, reads the result, and then decides again. This repetition is the agent loop, and it is also where the key design questions come from: when does the loop stop, and what should the model see on each iteration?

In the 4-tier architecture from the [previous article](../part6/14-four-tier-architecture-for-ai-agent-systems.md), the tier that answers these questions is T2 Orchestration. This article distinguishes workflows from autonomous agents and looks at which Spring AI parts fit the four stages of the loop. It then moves on to context engineering, which designs the information that goes into the loop, and to ReAct.

## Workflows and autonomous agents

In "Building Effective Agents", published in December 2024, Anthropic divides agentic systems into two kinds by their degree of autonomy.

- **Workflows**: LLMs and tools follow paths that developers define in code.
- **Autonomous agents**: The LLM decides which tools to use, in what order, and when to finish.

LLMs behave probabilistically, but a workflow fixes its execution path in code, so its flow is easy to predict and debug. In an autonomous agent, the model has control, so the tools it picks and their order can change even for the same question, and the model also decides when to end the loop. Among the book's examples, the Chapter 3 RAG CLI, which always searches and then answers in the same order, is close to a workflow. The CLIs in Chapters 4 and 5 also repeat tool execution and model calls, but they stop once they have answered a single request. Chapter 6 puts this loop at the center and adds planning, skills, delegation, approval, and observability to grow it into an agent system.

Autonomous agents suit open-ended requests that are hard to solve with a fixed procedure, and tasks where you cannot know the number of tool calls in advance. If the procedure and outcome are clear, if latency and cost need to be predictable, or if nondeterminism is a burden in the domain, a workflow is enough. Real systems use both together, for example by placing an autonomous agent inside one step of a workflow.

## Five workflow patterns

Anthropic describes five workflow patterns, and complete examples implemented with Spring AI are in the Agentic Patterns repository.

| Pattern | How it works | Good fit for |
| --- | --- | --- |
| Prompt chaining | Splits a task into steps and passes each step's output to the next step as input. You can put validation gates between steps. | Writing marketing copy, then translating it |
| Routing | Classifies the input and sends it to the prompt, tools, or model suited to its type. | Directing inquiries by type |
| Parallelization | Runs several LLM calls at the same time and aggregates the results in code. It comes in two forms: sectioning, which splits the task, and voting, which runs the same task several times. | Reviewing code from several security perspectives at once |
| Orchestrator-workers | An orchestrator LLM breaks the work into subtasks as it goes, hands them to worker LLMs, and combines the results. | Coding that changes multiple files, in-depth search |
| Evaluator-optimizer | A generator LLM revises its output based on feedback from an evaluator LLM until it meets the criteria. | Translation where nuance matters |

Parallelization and orchestrator-workers differ in who decides the subtasks: in parallelization the developer fixes them in code, while in orchestrator-workers the LLM decides them by looking at the request.

<figure class="wide-figure" markdown>
![Prompt chaining workflow](../assets/figures/fig6-1.png)
<figcaption>Prompt chaining workflow (Source: <a href="https://docs.spring.io/spring-ai/reference/api/effective-agents.html">Building Effective Agents</a>)</figcaption>
</figure>

Of these patterns, the example repository includes prompt chaining.

```java title="ChainWorkflow.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/local/ChainWorkflow.java:11:33"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/capability/local/ChainWorkflow.java)</span>

A for loop goes through the array of system prompts and inserts each step's response into the next input. The array and the loop decide the number and order of steps, and at each step the model only produces text without choosing what to do next. This is the deterministic control flow of a workflow.

## The autonomous agent loop

Whatever form they take, autonomous agents repeat four stages.

<figure class="wide-figure" markdown>
![How an autonomous agent works](../assets/figures/fig6-6.png)
<figcaption>How an autonomous agent works</figcaption>
</figure>

- **Plan**: Looks at the current context, such as the request and memory, and chooses the next action: a tool call, a follow-up question, or a final answer.
- **Act**: Calls a tool or produces a message. It may also call another agent.
- **Observe**: Collects the responses the environment returns, such as tool execution results.
- **Reflect**: Adds the results to the context and checks the termination condition, stopping if it is met and going back to planning if not.

The more of the four stages the model handles, the more autonomous the system becomes, and most real-world systems sit somewhere between a chatbot that a person instructs every time and a fully autonomous agent.

The stage most easily left out is reflect. If you pile up tool results as they arrive, the context quickly swells and the model can no longer pick out the important information. You need processing that compresses the results down to the essentials, or extracts the information you need and keeps it in a separate memory.

The foundation of an autonomous agent is using tools again and again based on the actual results the environment returns, not on the model's guesses. That is why writing clear tool descriptions and parameters is the starting point for reliability. There are two ways out of the loop. Stopping when the work is done or when the maximum number of iterations is reached leads to safety guards, and asking a person at a point where the agent finds it hard to decide on its own leads to [human-in-the-loop (HITL)](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md).

## Assembling the agent loop with Spring AI

Spring AI already has components to handle each stage of the loop, so you only need to rearrange the parts you learned in the earlier chapters, this time from the loop's point of view.

| Stage | What it does | Spring AI components |
| --- | --- | --- |
| Plan | Decides the next action | `ChatModel` |
| Act | Executes tools | `ToolCallback`, `@Tool`, `ToolCallingManager`, MCP client |
| Observe | Assembles the current context | `ChatClient`, `Advisor`, `ChatMemory`, `VectorStore` |
| Reflect | Processes results and decides when to stop | `Advisor`, `ToolCallingManager` |

If you write the loop yourself with `ToolCallingManager`, it becomes a while loop that repeatedly calls the model (plan), checks whether any tool calls remain (reflect), executes the tools (act), and updates the prompt with the conversation history containing the results (observe). This is the user-controlled approach covered in [Implementing Tools and Controlling Execution](../part4/10-tool-implementation-and-execution-control.md). Usually you leave this repetition to `ToolCallingAdvisor`, and the main agent in Chapter 6 is assembled that way too.

```java title="SpringAIAgent.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java:25:51"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/SpringAIAgent.java)</span>

Unlike `ChainWorkflow`, this code has no loop statement. It attaches behavior rules with `defaultSystem`, tools with `defaultTools`, and conversation history with `MessageChatMemoryAdvisor`, and the loop is run by the `ToolCallingAdvisor` that `run()` plugs in for each request. The next article explains what the order values in the comments (+200, +300) mean.

Step 1, `Ch6Step1_SpringAIAgent`, sends the request "Tell me the current time in Korea, check the stock of SKU-200, and then reserve 2 units." It needs the time lookup, stock lookup, and reservation tools, but the code does not say which tools to call in what order. The model decides. The second request, "What was the name of the product I just reserved?", is sent with the same conversation ID to check that chat memory carries over.

## From prompt engineering to context engineering

Prompt engineering is the craft of writing a single question well, using personas, few-shot examples, and step-by-step instructions. An agent keeps using tools, retries when something fails, and has to remember the reasons behind its earlier decisions, so the wording of a question alone is not enough. Context engineering is designing and managing the structure, order, and constraints of the input information, along with tool results, so that the model makes good decisions at every moment along the way. Think of it not as a knack for talking to the model but as designing the environment the model works in.

| Aspect | Prompt engineering | Context engineering |
| --- | --- | --- |
| Goal | Quality of a single answer | The whole workflow and state management |
| Main levers | Rewording, few-shot examples, personas | Memory, tool schemas, advisors, loops |
| Spring AI | `PromptTemplate` | `ChatClient`, `Advisor`, `ToolCallback`, `ChatMemory` |

The core agent's system prompt is a good example.

```yaml title="application.yml"
--8<-- "chapter6/src/main/resources/application.yml:10:16"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/resources/application.yml)</span>

This prompt is not a trick for a particular question but a set of behavior rules attached to every call. The rule to check tool results and then decide the next action defines how the loop runs, and the rule not to assert facts that have not been verified defines what the agent's decisions are based on. This value is read from the configuration file and passed to `defaultSystem` in `SpringAIAgent`, so you can adjust the agent's character without changing code.

## Chain of thought and reasoning models

Chain of thought (CoT) is a [prompt engineering technique](../part2/03-chatmodel-chatclient-and-prompt-engineering.md) that has the model reason step by step instead of jumping to a conclusion. In an agent, you go beyond a loose instruction like "think step by step" and put the checks to go through before calling a tool into the system message as a policy. For example, an agent that queries financial data would be allowed to call a tool only after checking the permission level, the query period, and whether personal information is included.

Open source models such as Qwen3 and gpt-oss now also have the reasoning ability to verify and plan on their own before answering. You do not need to tell these models separately to think. The developer's work shifts toward giving them decision criteria, writing accurate tool definitions, and weaving execution results and failure history into the environment for the next round of reasoning. Models cannot call databases or APIs directly, so the loop that turns reasoning into execution and returns the results to the context is still the system's responsibility.

The `qwen3.5:4b` model that the example repository uses also goes through a thinking phase before answering, and `ThinkTraceAdvisor` shows this thinking in the CLI.

```java title="ThinkTraceAdvisor.java"
--8<-- "chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/ThinkTraceAdvisor.java:44:72"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter6/src/main/java/kr/jmlab/spring/ai/agent/book/chapter6/orchestration/ThinkTraceAdvisor.java)</span>

`OllamaChatModel` puts thinking tokens under the `thinking` key in the metadata of each streaming chunk. The response aggregated per round does not keep this value intact, so this advisor reads the raw stream directly and prints the thinking tokens. It runs on every round inside the tool loop, so the CLI prints thinking, a tool call, more thinking, and the answer in turn. This flow, in which thinking and acting alternate, is ReAct, which comes next.

## ReAct: turning thought into action

The plan, act, observe, and reflect loop follows the ReAct pattern published in 2022, and most commercial agent frameworks use a variation of it. If chain of thought is a way to refine reasoning, ReAct is an execution structure that alternates between reasoning and acting.

<figure class="wide-figure" markdown>
![ReAct sequence, the way an agent acts](../assets/figures/fig6-8.png)
<figcaption>ReAct sequence, the way an agent acts</figcaption>
</figure>

The key is the boundary between the model and the system. The model only decides that it needs weather information (reason) and produces a request to call `getWeather("Seoul")` (act). Executing the tool (execution) and putting the result `{"temp":"20"}` into the context (observation) are the system's job. In Spring AI, `ToolCallingManager` and advisors handle this boundary, so once a developer registers a tool, the model and the framework take care of whether to use it, which arguments to call it with, and how to turn the result into an answer.

## Two areas where context is delivered

The context you design ends up in the model call as a system message, tool schemas, and a list of messages. LLMs do not remember state between calls, so `ChatMemory` adds the earlier conversation, and `VectorStore` and advisors find documents and add them. In Andrej Karpathy's analogy, the LLM is the CPU, the context window is the RAM, and context engineering is the operating system that decides what to load into memory and when. When agents fail, the cause often lies less in the model's intelligence than in needed information not reaching the context in time.

Looking one step closer, the elements of context fall into two areas. One influences the model's decisions through text instructions and tool schemas. The other is enforced by the agent system: which tools to expose, which files to read and when, and how to restrict permissions.

<figure class="wide-figure" markdown>
![Areas where the contents of the context are delivered and used](../assets/figures/fig6-9.png)
<figcaption>Areas where the contents of the context are delivered and used</figcaption>
</figure>

Instruction files such as AGENTS.md, which record build commands and coding conventions, are a good example. The file's contents are delivered to the model, but the system decides which files to look for, in what priority, and when to add them. Tools also span both areas. Their names, descriptions, and schemas become the basis for the model's decisions, while the system handles exposure filtering, execution, and permission restrictions. If you leave the delete tool's schema out entirely for a request from a user without delete permission, the model does not know the tool exists and has no definition to call. If writing "do not delete" in the prompt is a request, removing the schema is a block. It is not the whole of authorization, though: the execution side still has to check permissions, because a model can produce a tool name it was never shown, and Spring AI's tool resolver may resolve that name from the application context.

As the loop keeps running, context accumulates and can exceed the token limit, or the model can fail to use information placed in the middle of a long context, a problem known as lost in the middle. Strategies such as a sliding window, summarizing older conversation, and storing key facts can be implemented by combining `ChatMemory` and advisors. However, records of failed tool executions must be kept so that the model tries a different approach. Context is more than a conversation record: it is information that tells the agent what state it is in right now.

## Where this fits in the 4-tier architecture

The agent loop and context design covered in this article are the foundation of T2 Orchestration. The LLM's reasoning loop decides what to execute, when, and in what order, and designing what context to load into that loop is T2's job. The tools the loop calls belong to T3 Capability, and the model that makes the plans belongs to T4 Foundation. The next article, [Recursive Advisors and Tool Loop Control](../part6/16-recursive-advisor-and-tool-loop-control.md), looks at how Spring AI runs this loop inside the advisor chain.

## More in the book

!!! book "Book sections 6.1-6.2"
    - Spring AI implementation code for the routing, parallelization, orchestrator-workers, and evaluator-optimizer patterns
    - An example that writes the tool loop by hand with `ToolCallingManager` to see the four stages
    - A table mapping the features of Claude Code, OpenClaw, and Codex CLI to Spring AI core and community extensions
    - How the developer's responsibilities changed with the arrival of reasoning models
    - Examples of writing AGENTS.md and SKILL.md, and a table mapping the two areas of context to Spring AI

    [About the book](../book.md){ .md-button } [Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## References

- [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents): the distinction between workflows and autonomous agents
- [Building Effective Agents](https://docs.spring.io/spring-ai/reference/api/effective-agents.html): a guide to implementing the workflow patterns with Spring AI
- [Agentic Patterns](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns): complete example code for the workflow patterns
- [ReAct: Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629): the paper that proposed the ReAct pattern (2022)
- [Andrej Karpathy, context engineering](https://x.com/karpathy/status/1937902205765607626): the post that endorsed the term context engineering
