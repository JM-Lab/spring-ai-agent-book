---
title: "Appendix: Switching to OpenAI, AI Evaluation, and Connecting External AI Agents"
description: "Switch the examples from Ollama to OpenAI, strengthen the agent loop with LLM-as-a-judge evaluation, and connect the book's MCP servers to external agents."
tags:
  - Appendix
---

# Appendix: Switching to OpenAI, AI Evaluation, and Connecting External AI Agents

<div class="post-meta" markdown>
<span class="tier-chip t4">T4 Foundation</span> <span class="tier-chip tx">Cross-cutting concerns</span> Book appendices A-C | Example [`README.md`](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md) (in Korean)
</div>

All the exercises in the main chapters of the book run on local Ollama models. That way, you can follow along without an internet connection or extra cost. Appendices A-C go one step further. They try switching the model provider to OpenAI, scoring the agent's answers with an LLM, and connecting the MCP servers built in Chapters 5 and 6 to external AI agents such as Claude Code and Codex.

The three topics differ in nature, but they have one thing in common: each extends the structure already built without major changes. The model changes behind Spring AI's common API, evaluation attaches as a separate step apart from the generation flow, and external agents connect to the same servers by following the MCP standard. This article summarizes the key points of the three appendices in order.

## Setting up the hands-on environment with the OpenAI API

If your PC is barely capable of running local models, or you want to see answers from a larger commercial model, you can move the examples to the OpenAI API. Because the examples depend on Spring AI abstractions such as `ChatClient`, `EmbeddingModel`, `VectorStore`, and `ToolCallback`, you can leave most of the Java code as it is and change only the dependencies and configuration. Had the code been tied to a specific provider's SDK from the start, there would have been far more to fix when switching models or using several models together.

First, register your billing information on the OpenAI platform and create an API key. Test calls can add up to more than you expect, so set a monthly usage limit as well. You cannot view the key again after it is issued, so store it separately, and instead of writing the key value in configuration files, reference the `OPENAI_API_KEY` environment variable. For the dependency, replace the Ollama starter in `pom.xml` with the OpenAI starter, or keep both.

```xml title="pom.xml"
--8<-- "README.md:153:156"
```

In the configuration, change the values of `spring.ai.model.chat` and `spring.ai.model.embedding`, the model selection keys in Spring AI 2.0, from `ollama` to `openai`, and put the key and model names under `spring.ai.openai`.

```yaml title="application.yml"
--8<-- "README.md:162:172"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md#openai-api로-실행하기)</span>

The embedding settings are needed only for `chapter3`, `chapter5`, and `chapter6`, which use RAG. For `basic-chat`, `chapter2`, and `chapter4`, which use only chat, changing the `chat` side is enough. In Chapters 5 and 6, where the client and server are separate, also add the OpenAI settings to the server-side configuration (for Chapter 5, the server mode document in `application.yml`; for Chapter 6, `application-knowledge.yml`). `gpt-4.1-nano` and `text-embedding-3-small` were chosen not because they perform best, but because together they strike a balance that suits the exercises. Their latency and cost are predictable, and they handle everything from streaming to structured output, tool calling, and RAG. The gpt-5 series also works, but it comes with reasoning token charges and latency, so the README recommends the gpt-4 series for the exercises.

When switching, the step that is easy to overlook is startup. Adding the OpenAI starter also turns on OpenAI auto-configurations beyond chat and embedding, such as audio, and some of those beans require credentials at startup. So even a process that does not use OpenAI chat will not start without a key. Set `OPENAI_API_KEY` when you start the MCP servers from Chapters 5 and 6 as well, and if startup fails, check the environment variable first. Each chapter's README also lists caveats found while actually making the switch.

- **RAG similarity threshold**: The 0.50 on the Chapter 5 and 6 servers is tuned for `bge-m3`, so with `text-embedding-3-small`, search results tend to come back empty. Lowering it to around 0.3 is recommended.
- **Streaming and tool calling**: Examples that use both together, such as the final CLI in Chapter 4, fail with an error on OpenAI, so check the behavior with the non-streaming step examples.
- **Low-level calls**: The Chapter 4 `ch4-step4` example, which controls the tool execution loop directly, calls `ChatModel` without going through `ChatClient`, so its options object also has to be changed to `OpenAiChatOptions`. This is because the conversion from provider-neutral options to provider-specific options happens in the `ChatClient` layer.

## Verifying response quality with AI evaluation

An agent running to completion does not guarantee a good answer. If you do not measure what is right and what is wrong, you cannot set a direction for improvement either. The method Appendix B introduces is LLM-as-a-judge. An LLM in the evaluator role reads the response and judges whether it fits the intent of the question, whether it is consistent with the retrieved sources, and whether it makes up anything that is not in those sources. Because it can weigh meaning and facts the way a person reading the response would, it is mainly used to evaluate the quality of RAG and agents.

Evaluation in Spring AI starts from the `Evaluator` interface. Its `evaluate` method takes an `EvaluationRequest` and returns an `EvaluationResponse`. The request holds the user question, the list of source documents, and the model response to evaluate. From the result, you get whether it passed with `isPass()`, a score between 0.0 and 1.0 with `getScore()`, and the explanation the evaluator left with `getFeedback()`. Because the structure is this small, you can separate evaluation from the generation flow and attach it wherever you want. You can place it right after a RAG answer, run it only in integration tests, or apply it only to high-risk requests in production. The results are not merely recorded, either. The pass/fail result serves as a quality gate, and the score is used for regression checks and for comparing experiments. However, the two built-in evaluators return responses with empty feedback text, and the relevancy evaluation's score is either 0 or 1, so if you need guidance for improvement, you need an `Evaluator` implementation with an evaluation prompt you write yourself.

There are two built-in evaluators. Both have an LLM do the scoring with an evaluation prompt, so when you create one, you pass in a `ChatClient.Builder` for the model that will act as the judge. `RelevancyEvaluator` checks whether the question, the sources, and the answer fit together. When the retrieved documents only share keywords with the question, the answer may read smoothly but miss the point. You can catch such answers and have the system search again with more results or a revised query. `FactCheckingEvaluator` checks whether the claims in the answer are actually supported by the source documents. Even if the right document was found, when the model adds a revision date that is not in the document, the answer can pass the relevancy evaluation and still be caught by the fact-checking evaluation. That is why, for RAG, it is safer to look at both evaluations together.

Because evaluation calls a model again, it fits integration tests better than unit tests. The example repository likewise separates checks that need a live model into `*IT` integration tests, and leaves response quality for a person to judge by looking at the output. In production, you can automate evaluation, for example by deploying only prompts that pass a representative set of questions, or by sampling important responses and evaluating them in a nightly batch. Because the verdict also comes from an LLM, results can be unstable if you leave it to a small local model. It helps to separate the generation model from the evaluation model and use a more stable model for evaluation.

## Strengthening the agent loop with evaluation

Moving scoring from an after-the-fact review into the loop gives you the evaluator-optimizer workflow from the [Chapter 6 workflow patterns](../part6/15-agent-loop-and-context-engineering.md). The generator produces an answer, the evaluator reviews it against criteria, and if it does not pass, it goes back to the generator with feedback. For tasks where the passing criterion is whether the answer matches its sources, as in RAG, you can plug in `RelevancyEvaluator` or `FactCheckingEvaluator` as the judge instead of writing a new evaluator prompt.

The flow is simple. The agent produces an answer first, and the evaluator takes the question, the source documents, and the answer and makes a judgment. If `isPass()` is true, the loop stops. If it is false, an instruction containing the reason for the failure is appended to the question, and the agent runs again. Because the built-in evaluators leave the feedback empty, this instruction comes from `getFeedback()` of an evaluator you built yourself or from predefined wording. The number of iterations is kept within a set maximum number of attempts.

This changes the criterion for ending the loop. The loop does not stop because there are no more tools to call. It stops after checking whether the result has reached the goal. If the result falls short, you can also have the agent search again, call a different tool, or ask the user for more information. In effect, the loop is not left to the model's judgment alone, and the system shares control of it.

There is a cost. Each iteration involves both generation and evaluation, so the number of calls, tokens, and response time all go up together. This suits work that improves with repetition, such as polishing a translation, refining a report draft, research that needs several searches, and code generation and review, but it can be overkill for question answering where a single pass produces the answer. Just as you [put an iteration limit on the tool loop](../part6/16-recursive-advisor-and-tool-loop-control.md), clearly define where to stop with a maximum number of attempts, a minimum passing score, and a condition that applies the loop only to high-risk tasks.

## Connecting the book's MCP servers to external AI agents

The MCP servers from Chapters 5 and 6 are exposed over Streamable HTTP, so any agent that supports MCP can connect to the same servers, even if it is not the book's CLI client. The point of this exercise is less about getting the connection to work and more about seeing for yourself that the book's servers are standard implementations not tied to any particular product. Attaching the same server to several clients of different kinds shows what MCP means by interoperability.

There are three servers you can connect to. The Chapter 5 RAG server provides search and grounded answers, the Chapter 6 knowledge server provides `rag_answer_question`, and the Chapter 6 operations server provides `check_stock` and `place_purchase_order`. The RAG-based servers index documents while starting up, so they need Ollama's `bge-m3` embedding model. The exercise uses the knowledge server (8086), whose port does not overlap with the other servers.

```bash
# Run the Chapter 6 knowledge MCP server (endpoint http://localhost:8086/mcp)
cd chapter6
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=knowledge"

# In another terminal, register the server with Claude Code
claude mcp add --transport http book-knowledge http://localhost:8086/mcp
```

In Claude Code, a remote HTTP server is registered with the `claude mcp add --transport http` command. The name is up to you, but a name that shows the server's role, such as book-knowledge, is easier to manage. You can check the registered servers with `claude mcp list` and the connection status with `/mcp` during a conversation. Now, if you ask something like "Find the safety stock criteria in the inventory policy document," Claude Code finds the server's tool, calls it, and answers with the result. The structure of model, tools, and loop built in Chapter 6 runs the same way in an external agent.

Other clients differ only in how you give them the address. In Claude Desktop, register the `mcp-remote` bridge in `claude_desktop_config.json` and restart the app. The custom connectors under Settings > Connectors are not the route for this exercise: they connect from Anthropic's infrastructure rather than from your computer, so a localhost address does not work there. For OpenClaw, you pass the server name to the `openclaw mcp set` command along with JSON that contains the URL and transport. In Codex, the CLI and the IDE extension share the same configuration. You write the URL in `~/.codex/config.toml` or in the project's `.codex/config.toml`, and depending on the version, you can also register the server with the `codex mcp add` command.

```toml title="~/.codex/config.toml"
--8<-- "README.md:113:114"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/README.md#mcp-서버를-외부-ai-클라이언트에-연결하기)</span>

There are also a few things to check when connecting. The Chapter 5 server and the Chapter 6 operations server both use port 8085, so do not run them at the same time. When `place_purchase_order` is called on the operations server, the server asks for [user approval](../part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md) through MCP Elicitation before placing the order. With a client that does not support this feature, check first with a lookup tool such as `check_stock`. The example servers run without authentication and listen on every network interface, so for local-only practice you can bind them to loopback with `--server.address=127.0.0.1`, and before opening them to other networks, apply the [MCP security configuration](../part5/13-mcp-security-oauth2-and-jwt.md) first. If the server and client are not on the same machine or are separated into containers, replace localhost in the address with a host name the client can reach.

## Where this fits in the 4-tier architecture

Appendix A changes the model in the Foundation (T4). Because Orchestration (T2) and Capability (T3) above it rely on Spring AI's common API, a change of provider mostly comes down to changing dependencies and configuration. Appendix B uses evaluation to verify the quality of the responses an agent produces, and strengthens the agent loop by using that verdict as the criterion for stopping iterations and as input for the next attempt. In Appendix C, the Channel (T1) that users interact with changes from the book's CLI to Claude Code or Codex, and the external agent also takes over the loop that picks tools. Even so, MCP, the standard interface for calling capabilities, sits in between, so the MCP servers in the T3 position can be used as they are. The approval gate and security configuration you checked when connecting belong to governance and security, which the book groups as cross-cutting concerns. The next article, [Appendix: Spring AI Playground](../appendix/23-spring-ai-playground.md), introduces a separate open source tool that connects standards-compliant servers so you can inspect and test them in one place.

## More in the book

!!! book "Book appendices A-C"
    - Appendix A: The steps for issuing an API key and setting usage limits, and the habit of handling keys separately from code
    - Appendix B: Example code that sends evaluation requests with `RelevancyEvaluator` and `FactCheckingEvaluator`, and a summary of `EvaluationResponse` values
    - Appendix B: Example code for an evaluator-optimizer loop that plugs in an evaluator as the judge
    - Appendix C: The connector approach in Claude Desktop and an example `claude_desktop_config.json` configuration
    - Appendix C: Why Codex and OpenClaw are introduced together, and how to make use of each client once connected

    [About the book](../book.md){ .md-button } [Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## References

- [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html): the Spring AI evaluation API reference
- [Evaluator-Optimizer Pattern](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns/evaluator-optimizer): the evaluator-optimizer example in the Spring AI examples repository
- [Connect Claude Code to tools via MCP](https://code.claude.com/docs/en/mcp): Claude Code documentation on connecting MCP servers
- [Get started with custom connectors using remote MCP](https://support.claude.com/en/articles/11175166-get-started-with-custom-connectors-using-remote-mcp): a guide to custom connectors built on remote MCP
- [Connectors overview](https://claude.com/docs/connectors/overview): an overview of Claude connectors
- [Model Context Protocol - Codex](https://developers.openai.com/codex/mcp): Codex documentation on MCP configuration
- [OpenClaw Docs - MCP](https://docs.openclaw.ai/cli/mcp): OpenClaw documentation on the MCP commands
