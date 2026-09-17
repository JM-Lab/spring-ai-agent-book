---
title: About the Book
description: "The Korean book The Complete Guide to AI Agent Development with Spring AI 2.0 (Jemin Huh, Wikibooks, 2026): contents, setup, example code, error reports."
search:
  boost: 0.5   # Lower the search weight so this table of contents page does not rank above the articles
---

# The Complete Guide to AI Agent Development with Spring AI 2.0

<div class="book-card" markdown>
<div class="cover-wrap"><img class="off-glb" src="../../assets/img/cover.jpg" alt="Cover of the Korean book The Complete Guide to AI Agent Development with Spring AI 2.0"></div>
<div markdown>
**Building Enterprise AI Agent Systems with Java and Spring**
<br><span lang="ko">스프링 AI 2.0으로 배우는 AI 에이전트 개발 완벽 가이드</span> (Korean title)

By Jemin Huh, Wikibooks Open Source & Web Series, published September 17, 2026, 692 pages, ISBN 9791158396961, in Korean

[Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary } [Example code](https://github.com/JM-Lab/spring-ai-agent-book){ .md-button }
</div>
</div>

## What this book is about

AI agents are evolving beyond chatbots that answer questions into systems that search enterprise documents, call tools in business systems, and carry out real work over multiple steps. To apply such systems to real services, the quality of the model's answers alone is not enough. You need to be able to control access to enterprise data, manage tool execution safely, require human approval for important operations, and observe and evaluate the entire execution process.

This book explains LLM chat, RAG, tool calling, MCP, and AI agents with Spring AI 2.0, in the order the technology evolved. It guides developers who are familiar with Java and Spring in building enterprise AI agent systems while keeping their current application architecture.

## Growing a single chat CLI from start to finish

Instead of listing examples feature by feature, the book extends a single chat CLI project in every chapter. It starts a conversation with `ChatClient`, adds RAG and a vector database to put enterprise documents to use, and connects external systems through tool calling and MCP. Finally, it defines AI agent systems in terms of a 4-tier architecture of Channel, Orchestration, Capability, and Foundation. Following that design, it completes an AI agent CLI that combines file system tools, MCP tools, and a tool search meta-tool to pick and use the features it needs on its own.

Every exercise runs on local LLMs with Ollama, so you can follow along without API keys or usage costs. The appendix also covers how to switch to OpenAI models.

## What this book covers

- From the core concepts of LLMs and AI agents to AI application development on Spring AI 2.0
- Prompt engineering, structured output, chat memory, and the advisor chain
- Extracting, transforming, and loading documents, plus vector databases
- Designing and implementing basic and advanced RAG
- Tool calling that connects Java methods and external systems
- Implementing MCP (Model Context Protocol) clients and servers
- Extending agent capabilities with a tool search meta-tool and MCP tools
- Implementing AI agents that use Agent Skills and subagents
- A 4-tier architecture for AI agent systems made up of Channel, Orchestration, Capability, and Foundation
- Enterprise AI agent systems with human approval, evaluation and verification, and observability

## Who this book is for

This book is for developers who have built applications with Java and Spring. You do not need any experience with AI or machine learning. It starts with how LLMs work and builds up step by step, so if you have built a web application or a REST API with Spring, you can follow along comfortably.

## Hands-on environment

| Item | Baseline |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.0.x |
| Spring AI | 2.0.0 GA |
| Chat model | Ollama `qwen3.5:4b` |
| Embedding model | Ollama `bge-m3` |

## Full table of contents

Click a chapter title to expand its list of sections. Each chapter also links to the articles on this site that summarize it.

??? abstract "Chapter 1. AI Agents: The Start of a New Paradigm"
    - 1.1 Beyond LLMs, into the Era of 'Agents' That Work on Their Own
    - 1.2 Adopting AI Technology in Enterprise Environments
        - 1.2.1 The Python-Centric AI Development Ecosystem
        - 1.2.2 The Shift in the Java AI Development Ecosystem: The Beginning of New Possibilities
    - 1.3 Introducing Spring AI
        - 1.3.1 The Goals of Spring AI
        - 1.3.2 The Role and Advantages of Spring AI in the Spring Ecosystem
        - 1.3.3 The Evolution of Spring AI: From 1.0 for AI Integration to 2.0 for Agents
    - 1.4 The Goals and Structure of This Book
        - 1.4.1 A Practical AI Agent Development Guide for Java Developers

    Articles on this site: [From LLM Calls to AI Agents: Why Spring AI for Java Developers](part1/01-from-llm-calls-to-ai-agents-why-spring-ai.md), [From Spring AI 1.0 to 2.0: What Changed](part1/02-whats-new-in-spring-ai-2-0.md)

??? abstract "Chapter 2. The Spring AI Framework"
    - 2.1 Key Features and the Design Direction of the AI Model API
        - 2.1.1 Key Features
        - 2.1.2 AI Model API Design
    - 2.2 Setting Up the Development Environment
        - 2.2.1 Installing Ollama and Setting Up the Local Environment
        - 2.2.2 Basic Project Setup
        - 2.2.3 Developing a Simple AI Application
    - 2.3 ChatModel and ChatClient
        - 2.3.1 Separating Roles: Driver and Client
        - 2.3.2 ChatModel: Communication Methods and Response Data
        - 2.3.3 ChatResponse and Response Metadata
        - 2.3.4 Using the ChatClient API
        - 2.3.5 Advanced Features of ChatClient and Extending to AI Agents
    - 2.4 Prompt Engineering
        - 2.4.1 Prompt Design Philosophy
        - 2.4.2 The Message API and Role-Based Design
        - 2.4.3 PromptTemplate: Managing Dynamic Prompts
        - 2.4.4 Advanced Templating Techniques
        - 2.4.5 Writing Effective Prompts
        - 2.4.6 Using ChatOptions
        - 2.4.7 Using Model-Specific Options and Configuration Strategies
        - 2.4.8 Various Prompt Engineering Techniques
    - 2.5 Tokens
        - 2.5.1 Token Structure and the Developer's Perspective
        - 2.5.2 The Evolution of Korean Token Efficiency
        - 2.5.3 Hybrid Prompt Strategy
    - 2.6 Structured Output
        - 2.6.1 Framework-Driven Prompt Engineering
        - 2.6.2 The Architecture of Structured Output
        - 2.6.3 StructuredOutputConverter Implementations
        - 2.6.4 Using the Various StructuredOutputConverter Implementations
        - 2.6.5 Provider-Native Structured Output
    - 2.7 Chat Memory and Context Management
        - 2.7.1 The ChatMemory Interface and Its Default Implementation
        - 2.7.2 A Detailed Analysis of ChatMemoryRepository by Storage Type
        - 2.7.3 Switching Storage Flexibly with ChatMemoryRepository
        - 2.7.4 Patterns for Combining Short-Term and Long-Term Memory
        - 2.7.5 Managing Short-Term Memory Manually
    - 2.8 The Advisor Chain
        - 2.8.1 Class Structure of the Advisors API
        - 2.8.2 Advisor Execution Flow and Stack Structure
        - 2.8.3 Synchronous vs. Streaming Processing
        - 2.8.4 Implementing Advisors and Built-In Advisors
        - 2.8.5 Advisor-Based Control: Request Rejection and Fallback
        - 2.8.6 Advisor Implementation Principles and Ordering Strategy
        - 2.8.7 Spring AI Advisors: The Core of the Framework and Its Extensibility
    - 2.9 AI Chatbot CLI Project
        - 2.9.1 When CLI Meets AI
        - 2.9.2 Project Structure and Configuration
        - 2.9.3 Implementing the Chat CLI Step by Step
        - 2.9.4 Developing the Final CLI Chatbot
        - 2.9.5 Summary

    Articles on this site: [ChatModel, ChatClient, and Prompt Engineering](part2/03-chatmodel-chatclient-and-prompt-engineering.md), [Tokens and Structured Output: Getting Model Answers as Java Objects](part2/04-tokens-and-structured-output.md), [Chat Memory and the Advisor Chain: The Core of Spring AI](part2/05-chat-memory-and-advisor-chain.md)

??? abstract "Chapter 3. Spring AI and RAG"
    - 3.1 Why RAG Emerged and Its Architecture
        - 3.1.1 The Structural Limitations of LLMs and the Data Gap
        - 3.1.2 Technologies for Filling the Data Gap: Fine-Tuning vs. Injecting Knowledge into the Prompt
        - 3.1.3 Spring AI's RAG Architecture
        - 3.1.4 The ETL Pipeline Framework
    - 3.2 Implementing DocumentReader for the ETL Pipeline
        - 3.2.1 Extracting from Text Files
        - 3.2.2 Extracting from JSON
        - 3.2.3 Extracting from PDF
        - 3.2.4 Extracting from Markdown
        - 3.2.5 Extracting from HTML
        - 3.2.6 General-Purpose Document Extraction
        - 3.2.7 Implementing a Custom DocumentReader
    - 3.3 Implementing DocumentTransformer for the ETL Pipeline
        - 3.3.1 Transforming and Cleaning Documents
        - 3.3.2 Unifying Metadata Formats
        - 3.3.3 Adding Keywords to Metadata
        - 3.3.4 Adding Contextual Summaries
        - 3.3.5 Implementing a Custom DocumentTransformer
        - 3.3.6 Integrating DocumentTransformers into a Pipeline
    - 3.4 Implementing DocumentWriter for the ETL Pipeline
        - 3.4.1 Loading into Files
        - 3.4.2 Implementing a Custom DocumentWriter
    - 3.5 Embedding Models
        - 3.5.1 What Embeddings Are and How They Work
        - 3.5.2 The Structure of the EmbeddingModel API
        - 3.5.3 Ways to Run Embedding Models: Cloud and Local
        - 3.5.4 CPU-Based Local Embedding Model Performance and Embedding Models That Support Korean
    - 3.6 Vector Databases
        - 3.6.1 The VectorStore Interface in Detail
        - 3.6.2 Search Requests (SearchRequest)
        - 3.6.3 Metadata Filters
        - 3.6.4 Vector Database Schemas
        - 3.6.5 Batch Loading Large Volumes of Documents
        - 3.6.6 Managing the Data Lifecycle of Vector Databases (Deletion and Updates)
        - 3.6.7 Various Vector Databases
        - 3.6.8 The Built-In In-Memory Vector Database (SimpleVectorStore)
    - 3.7 The Spring AI RAG Framework
        - 3.7.1 The Evolution and Paradigms of RAG
        - 3.7.2 Implementing Naive RAG
        - 3.7.3 Implementing Advanced RAG
        - 3.7.4 The Modules That Make Up Modular RAG
        - 3.7.5 Implementing the Pre-Retrieval Stage of Modular RAG
        - 3.7.6 Implementing the Retrieval, Post-Retrieval, and Generation Stages of Modular RAG
        - 3.7.7 Modular RAG Orchestration
    - 3.8 RAG AI Chatbot CLI Project
        - 3.8.1 Project Goals and Configuration
        - 3.8.2 Implementing the Offline ETL Pipeline
        - 3.8.3 Implementing the Runtime RAG Pipeline
        - 3.8.4 Developing the RAG CLI Chatbot

    Articles on this site: [RAG Architecture and the ETL Pipeline: Turning Documents into Knowledge](part3/06-rag-architecture-and-etl-pipeline.md), [Embedding Models and Vector Databases](part3/07-embedding-models-and-vector-stores.md), [From Naive RAG to Modular RAG: The Spring AI RAG Framework](part3/08-from-naive-rag-to-modular-rag.md)

??? abstract "Chapter 4. Tool Calling"
    - 4.1 The Tool Calling Environment for AI Models
        - 4.1.1 The Arrival of Tool Calling: LLMs Connect to the Real World
    - 4.2 Designing Tool Calling
        - 4.2.1 Characteristics of Tool Calling in Spring AI
        - 4.2.2 How Tool Calling Proceeds
        - 4.2.3 Tool Definitions
        - 4.2.4 Tool Context and Return Direct
        - 4.2.5 Tool Call Result Conversion
    - 4.3 Implementing Tools
        - 4.3.1 Implementing Methods as Tools
        - 4.3.2 Implementing Functions as Tools
        - 4.3.3 Comparing and Choosing Tool Implementation Approaches
        - 4.3.4 How to Pass Tools to AI Models
    - 4.4 Executing Tools
        - 4.4.1 The ToolCallingManager API
        - 4.4.2 The Internals of DefaultToolCallingManager
        - 4.4.3 Framework-Controlled Tool Execution
        - 4.4.4 Advisor-Controlled Tool Execution
        - 4.4.5 User-Controlled Tool Execution
    - 4.5 Tool-Enabled AI Chatbot CLI Project
        - 4.5.1 Project Goals and Configuration
        - 4.5.2 Implementing the Tool Calling CLI Step by Step
        - 4.5.3 Developing the Tool-Enabled AI Chatbot CLI

    Articles on this site: [Designing Tool Calling: How LLMs Connect to the Real World](part4/09-tool-calling-design-in-spring-ai.md), [Implementing Tools and Controlling Execution: From @Tool to ToolCallingManager](part4/10-tool-implementation-and-execution-control.md)

??? abstract "Chapter 5. Spring AI MCP"
    - 5.1 MCP Basics (Host/Client/Server, Primitives, Transports)
        - 5.1.1 MCP Architecture
        - 5.1.2 The Java MCP Stack Architecture
        - 5.1.3 The Java MCP Client-Server Architecture
        - 5.1.4 Direct Tool Calling vs. MCP Tool Calling
    - 5.2 Spring AI MCP Client
        - 5.2.1 MCP Client Boot Starter
        - 5.2.2 Common MCP Client Configuration
        - 5.2.3 MCP Client STDIO Configuration
        - 5.2.4 MCP Client Configuration for Remote Servers
        - 5.2.5 Configuring Remote Server Security with the MCP Client's headers
        - 5.2.6 Implementing MCP Client Features
        - 5.2.7 MCP Client Annotations
    - 5.3 Spring AI MCP Server
        - 5.3.1 MCP Server Boot Starter
        - 5.3.2 Common MCP Server Configuration
        - 5.3.3 MCP Server Protocol Configuration
        - 5.3.4 Implementing MCP Server Tools
        - 5.3.5 Implementing Various MCP Server Features
        - 5.3.6 Implementing Bidirectional MCP Server-Client Features
        - 5.3.7 MCP Server Annotations
        - 5.3.8 Implementing Tools with MCP Server Annotations
        - 5.3.9 Implementing Tools with MCP Server Annotations for Each Server Type
        - 5.3.10 Implementing Additional Features with MCP Server Annotations
        - 5.3.11 Implementing Additional Features with MCP Server Annotations for Each Server Type
    - 5.4 Spring AI MCP Security
        - 5.4.1 The MCP Security Environment
        - 5.4.2 MCP Security in Spring AI
        - 5.4.3 Common MCP Client Security Configuration
        - 5.4.4 Implementing MCP Client Security in Detail
        - 5.4.5 Configuring MCP Server Security
        - 5.4.6 Configuring the MCP Authorization Server
    - 5.5 MCP-Based AI Chatbot CLI Project
        - 5.5.1 Project Goals and Configuration
        - 5.5.2 Implementing the MCP Server Step by Step
        - 5.5.3 Implementing the MCP Client Step by Step
        - 5.5.4 Implementing the MCP-Based AI Chatbot CLI

    Articles on this site: [MCP Basics and the Spring AI MCP Client](part5/11-mcp-basics-and-spring-ai-mcp-client.md), [Building an MCP Server with Spring AI: From Boot Starters to Annotations](part5/12-building-an-mcp-server-with-spring-ai.md), [MCP Security: Protecting MCP Clients and Servers with OAuth2 and JWT](part5/13-mcp-security-oauth2-and-jwt.md)

??? abstract "Chapter 6. AI Agents"
    - 6.1 AI Agents and the Agent Loop
        - 6.1.1 Workflows and Autonomous Agents
        - 6.1.2 Workflow Patterns
        - 6.1.3 Autonomous Agents
        - 6.1.4 How Spring AI Supports Autonomous Agent Development
        - 6.1.5 Implementing Practical AI Agents
    - 6.2 Context Engineering
        - 6.2.1 From Prompt Engineering to Context Engineering
        - 6.2.2 Chain-of-Thought Prompt Design and Reasoning AI Models
        - 6.2.3 ReAct: How Agents Act
        - 6.2.4 LLMs and Agent Systems That Use Context Engineering
    - 6.3 Recursive Advisors and Tool Loop Control
        - 6.3.1 Two Approaches to the Tool Loop
        - 6.3.2 The Processing Flow of Recursive Advisors
        - 6.3.3 Controlling the Tool Loop with ToolCallingAdvisor
        - 6.3.4 ToolCallingAdvisor Hooks and Tool Argument Augmentation
        - 6.3.5 The Structured Output Validation and Self-Correction Loop
    - 6.4 Developing Spring AI Agents
        - 6.4.1 The Structure of AI Agent Services: Claude Code, Codex, OpenCode, and OpenClaw
        - 6.4.2 Multi-Agent Systems and Standards for Connecting Agents
        - 6.4.3 Spring AI Agent Architecture and Implementation
        - 6.4.4 Human-in-the-Loop (HITL) Workflows: Controlling Tool Permissions with MCP Elicitation
        - 6.4.5 Dynamic Tool Discovery
    - 6.5 Extending Spring AI Agents with Community Support
        - 6.5.1 Reusable Agent Skills
        - 6.5.2 A Tool That Asks Instead of Assuming (AskUserQuestionTool)
        - 6.5.3 A Tool for Writing Task Plans (TodoWriteTool)
        - 6.5.4 Subagent Orchestration for Multi-Agent Support
    - 6.6 Enterprise Spring AI Agent CLI Project
        - 6.6.1 Enterprise Spring AI Agent Systems
        - 6.6.2 Enterprise Spring AI Agent CLI System Design and Implementation Plan
        - 6.6.3 Implementing the Spring AI Agent CLI
        - 6.6.4 Extending Spring AI Agent Capabilities with MCP Servers
        - 6.6.5 Extending to a Multi-Agent System
        - 6.6.6 Extending Orchestration with Meta-Tools
        - 6.6.7 Integration and Observability

    Articles on this site: [4-Tier Architecture for AI Agent Systems](part6/14-four-tier-architecture-for-ai-agent-systems.md), [The Agent Loop and Context Engineering](part6/15-agent-loop-and-context-engineering.md), [Recursive Advisors and Tool Loop Control](part6/16-recursive-advisor-and-tool-loop-control.md), [Spring AI Agent Architecture, Implementation, and Dynamic Tool Discovery](part6/17-spring-ai-agent-architecture-and-dynamic-tool-discovery.md), [Human-in-the-Loop (HITL): An Approval Gate with MCP Elicitation](part6/18-human-in-the-loop-approval-gate-with-mcp-elicitation.md), [Agent Skills: Extending Agent Capabilities with Reusable Skills](part6/19-agent-skills-extending-capabilities.md), [An Enterprise Spring AI Agent CLI: Multi-Agent and Meta-Tool Orchestration](part6/20-enterprise-spring-ai-agent-cli-multi-agent-and-meta-tools.md), [Observability for AI Agents: Micrometer, OTel GenAI Semantic Conventions, and OTLP](part6/21-observability-for-ai-agents-micrometer-otel-otlp.md)

??? abstract "Appendix"
    - Appendix A. Setting Up the Hands-On Environment with the OpenAI API
    - Appendix B. Verifying Response Quality with AI Evaluation and Strengthening the Agent Loop
    - Appendix C. Connecting to External AI Agents
    - Appendix D. Spring AI Playground

    Articles on this site: [Switching to OpenAI, AI Evaluation, and Connecting External AI Agents](appendix/22-openai-evaluation-and-external-agents.md), [Spring AI Playground](appendix/23-spring-ai-playground.md)

## Example code

The hands-on project for each chapter is in the example repository. Each chapter folder is an independent Maven project, so you can run it directly inside that folder.

- Repository: [github.com/JM-Lab/spring-ai-agent-book](https://github.com/JM-Lab/spring-ai-agent-book)
- ZIP download: [main.zip](https://github.com/JM-Lab/spring-ai-agent-book/archive/refs/heads/main.zip)

## Reporting errors

If you find a mistake in the book or on this site, or get stuck running an example, please report it by opening an [issue](https://github.com/JM-Lab/spring-ai-agent-book/issues) in the example repository. Confirmed reports are reflected in the relevant articles.
