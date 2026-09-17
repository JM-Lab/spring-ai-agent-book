---
title: "From Naive RAG to Modular RAG: The Spring AI RAG Framework"
description: "How RAG evolved: Naive RAG with QuestionAnswerAdvisor, assembling modules with RetrievalAugmentationAdvisor, and the limits of linear orchestration."
tags:
  - Chapter 3
---

# From Naive RAG to Modular RAG: The Spring AI RAG Framework

<div class="post-meta" markdown>
<span class="tier-chip t3">T3 Capability</span> Book sections 3.7-3.8 | Example [`chapter3`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter3)
</div>

The previous two articles processed documents with an [ETL pipeline](../part3/06-rag-architecture-and-etl-pipeline.md), created vectors with an [embedding model](../part3/07-embedding-models-and-vector-stores.md), and loaded them into a vector database. The knowledge base is ready, but knowledge that just sits in a store does not change any answers. For the model to use that knowledge, the relevant documents have to be pulled out and put into the prompt the moment a user asks a question.

Spring AI implements this process as an advisor rather than as a separate system. Just as the [chat memory advisor](../part2/05-chat-memory-and-advisor-chain.md) added the earlier conversation to the request, a RAG advisor retrieves relevant documents and inserts them into the prompt before the request goes to the model. This article first looks at how RAG has evolved through Naive, Advanced, and Modular RAG, then implements each approach with `QuestionAnswerAdvisor` and `RetrievalAugmentationAdvisor`. Finally, it points out the limits of a pipeline that only runs in a fixed order and introduces the Chapter 3 hands-on project.

## The evolution of the RAG paradigm

RAG started as a technique for making up for two weaknesses of LLMs: hallucination and the knowledge cutoff. As its uses broadened, its structure also changed to fit the nature of the data and the requirements of each service. In a 2024 paper, Gao et al. divided this progression into Naive, Advanced, and Modular RAG, and proposed a structure that breaks RAG techniques down into modules and reassembles them as needed.

<figure class="wide-figure" markdown>
![Comparison of RAG paradigms evolving from Naive to Advanced to Modular](../assets/figures/fig3-7.png)
<figcaption>Comparison of RAG paradigms evolving from Naive to Advanced to Modular (Source: <a href="https://arxiv.org/html/2407.21059v1">Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks</a>)</figcaption>
</figure>

Naive RAG embeds the question, finds the top-k documents with the highest similarity, and sends them to the LLM by putting them into the prompt as they are. Despite its name, it is a basic approach that is widely used in practice. With few steps, it responds quickly and keeps token costs low, and for data with a clear structure and little duplication, such as internal company policies or FAQs, it is often enough. Spring AI's `VectorStoreChatMemoryAdvisor` also retrieves past conversations this way.

Its limits show up as data grows large and complex. With an ambiguous question that contains vague references, such as "What do I do if that doesn't work?", vector similarity alone has a hard time finding the right documents. If documents that score high but are actually unrelated get mixed in, the model may give an off-target answer, and because models tend to miss content in the middle of long inputs (lost in the middle), putting in more documents is not a solution either.

Advanced RAG reduces these limits by adding processing steps before and after retrieval. Before retrieval, it rewrites an ambiguous question into a sentence that works better for search, expands it into several similar questions, or translates it into the language of the documents. After retrieval, it reranks the results with a more sophisticated model such as a cross-encoder, removes sentences unrelated to the question, and discards documents that score below a cutoff.

However, implementing all of this processing yourself quickly bloats the pipeline code. Modular RAG splits functions such as rewriting, retrieval, and reranking into independent modules so that you assemble only the ones you need, and Spring AI's RAG framework follows this structure. The indexing stage in the figure is handled offline by the ETL pipeline from the earlier articles, and the RAG framework, implemented as advisors, takes care of the stages that follow.

## Naive RAG with QuestionAnswerAdvisor

You can build Naive RAG with a single `QuestionAnswerAdvisor` from the `spring-ai-vector-store-advisor` module. It is an advisor that searches with the question, without rewriting or post-processing, and puts the documents it finds into the prompt.

```java title="NaiveRagService.java"
--8<-- "chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/NaiveRagService.java:17:37,54:64"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/NaiveRagService.java)</span>

The `SearchRequest` defined in the constructor is fixed in the advisor. This service searches for the top 6 documents with a similarity of 0.8 or higher under the condition `category == 'tech_docs'`. `askWithFilter()` passes a filter with each request through the `QuestionAnswerAdvisor.FILTER_EXPRESSION` parameter, overriding the fixed filter. You use this approach when the search scope needs to be divided by user permissions or tenant.

The default prompt template that combines the question with the retrieved documents is in English, so the model may sometimes answer a Korean question in English. When you change the template to Korean, be sure to keep the `query` placeholder, where the question goes, and the `question_answer_context` placeholder, where the retrieved documents go.

If you add code that wraps `QuestionAnswerAdvisor` every time you need query rewriting or translation, the structure quickly gets complicated. `RetrievalAugmentationAdvisor` works as the same Naive RAG when you plug in just a retrieval module, and lets you add modules one at a time when you need them. That is why the book recommends designing real-world projects around this advisor from the start.

## The modules that make up RetrievalAugmentationAdvisor

`RetrievalAugmentationAdvisor` in the `spring-ai-rag` module defines four stages (Pre-Retrieval, Retrieval, Post-Retrieval, and Generation) and accepts modules for each stage through interfaces. You can use the built-in implementations or swap in your own.

| Stage | Interface | Configuration method | Built-in implementations |
| --- | --- | --- | --- |
| Pre-Retrieval | `QueryTransformer` | `queryTransformers()` | `RewriteQueryTransformer`, `CompressionQueryTransformer`, `TranslationQueryTransformer` |
| Pre-Retrieval | `QueryExpander` | `queryExpander()` | `MultiQueryExpander` |
| Retrieval | `DocumentRetriever` | `documentRetriever()` | `VectorStoreDocumentRetriever` |
| Retrieval | `DocumentJoiner` | `documentJoiner()` | `ConcatenationDocumentJoiner` |
| Post-Retrieval | `DocumentPostProcessor` | `documentPostProcessors()` | None (implement your own) |
| Generation | `QueryAugmenter` | `queryAugmenter()` | `ContextualQueryAugmenter` |

Pre-Retrieval modules refine the question so that it works better for search. `QueryTransformer` implementations call an LLM internally to transform the question. `CompressionQueryTransformer` combines the conversation history and the current question into a standalone question. For example, after a question about the capital of Denmark, it turns "What is the second-largest city there?" into "What is the second-largest city in Denmark?" `RewriteQueryTransformer` removes unnecessary parts such as greetings and rewrites the question around search terms, and `TranslationQueryTransformer` translates the question into the language the documents are stored in. It helps to set the temperature to 0.0 so that the transformation results stay consistent. `MultiQueryExpander` expands one question into several differently worded questions, so the search also finds documents that a single wording would miss.

In the Retrieval stage, `VectorStoreDocumentRetriever` supports a similarity threshold, top-k, and metadata filters. To change the filter per request, you use the `VectorStoreDocumentRetriever.FILTER_EXPRESSION` key. It is a different constant from the one in `QuestionAnswerAdvisor`, so if you use the wrong one, the filter may not be applied. When a question is expanded into several, the search also returns several sets of results, and `ConcatenationDocumentJoiner` concatenates them into a single list while filtering out duplicate documents. `DocumentPostProcessor`, which handles Post-Retrieval, is where you filter out less relevant documents or reorder them. It has no built-in implementation, so you implement it to fit your service. In the Generation stage, `QueryAugmenter` combines the remaining documents with the question to complete the final prompt.

## Assembling an Advanced RAG pipeline

`AdvancedRagService` in the example repository is an Advanced RAG setup with a module plugged into each of the four stages. It starts by creating the Pre-Retrieval, Retrieval, and Post-Retrieval modules.

```java title="AdvancedRagService.java"
--8<-- "chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java:41:57"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java)</span>

`RewriteQueryTransformer` calls the LLM through a cloned `ChatClient.Builder` to rewrite the question. The `targetSearchSystem` value is the name of the search target that goes into the rewrite prompt. To secure enough candidates, the retriever sets a low threshold of 0.5 and fetches up to the top 10. The post-processor is a `DocumentPostProcessor` written as a lambda that keeps only documents whose `isActive` metadata is true.

```java title="AdvancedRagService.java"
--8<-- "chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java:59:92"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/examples/AdvancedRagService.java)</span>

`ContextualQueryAugmenter` gets a Korean template. The retrieved documents go into the `{context}` slot and the question goes into the `{query}` slot. With `allowEmptyContext(false)`, the augmenter sends `emptyContextPromptTemplate` instead of the question when the search finds nothing, so rather than answering without evidence, the model guides the user to ask again within the scope of searchable documents. If you set it to true instead, the original question is sent as is even when there are no search results, and the model answers without documents. The default templates are in English and quite restrictive, so for a Korean-language service, it helps to replace both templates.

Finally, the modules are passed to the `RetrievalAugmentationAdvisor` builder in stage order. The Post-Retrieval stage gets the `isActive` filter along with `KeywordFilteringPostProcessor`, a custom post-processor that, when the question contains `긴급` ("urgent"), keeps only documents whose content also contains `긴급`. Once the finished advisor is added through `defaultAdvisors()`, every request sent through this `ChatClient` goes through the four stages.

## Running the pipeline and checking the search results

```java title="Ch3Step6_AdvancedRag.java"
--8<-- "chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step6_AdvancedRag.java:29:41"
```
<span class="code-link">[View full code](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step6_AdvancedRag.java)</span>

The calling side has no search code. `Ch3Step6_AdvancedRag` just takes a question and prints the response that `ragService.stream()` streams back, while query rewriting, retrieval, post-processing, and prompt augmentation all happen inside the advisor. The flip side is that the intermediate results stay hidden, so [`Ch3Step5_AdvancedRagModules`](https://github.com/JM-Lab/spring-ai-agent-book/blob/main/chapter3/src/main/java/kr/jmlab/spring/ai/agent/book/chapter3/Ch3Step5_AdvancedRagModules.java) prints the pipeline configuration and shows the retrieved documents separately. The `AdvancedRagService.retrieve()` method it uses runs a vector search with the original question, without rewriting, and then applies the same threshold and the two post-processing rules. So the documents shown on screen may differ from the evidence behind the actual answer, which is retrieved with the rewritten question. In the book's run, the question "What information should an urgent incident response document record?" retrieves a chunk of a policy document that says to record when the incident occurred and the scope of its impact.

The final CLI also shows the retrieved documents this way before the answer. When an answer is wrong, this serves as a reference for gauging whether retrieval was already off, but to see the actual evidence precisely, you need a separate record of the documents the advisor retrieved.

## The limits of linear orchestration

<figure class="wide-figure" markdown>
![RAG flow in the linear pattern](../assets/figures/fig3-8.png)
<figcaption>RAG flow in the linear pattern (Source: <a href="https://arxiv.org/html/2407.21059v1">Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks</a>)</figcaption>
</figure>

A pipeline built with `RetrievalAugmentationAdvisor` always runs its modules in the same order. The Modular RAG paper calls this structure the linear pattern. Because it takes the same path every time regardless of what the question is, it searches first even when it receives a greeting like "Hi there". Its behavior is easy to predict, but it cannot change its path to fit the situation. The paper argues that a system needs three patterns beyond the linear structure to become more mature. Conditional execution decides whether to search based on the type of question. Routing chooses between paths such as an internal wiki and web search. A loop rewrites the search terms and searches again when the results fall short.

This pipeline is a chain of responsibility fixed in code, so it has no step for making such decisions. You can also build conditional branching with a custom advisor or routing code, but to let the model decide whether to search, you provide search as a tool that the LLM chooses to use rather than as a fixed step. What decides the flow then shifts from the developer's Java code to the model's reasoning, and a structure that searched on every request becomes one that searches only when needed. The linear pattern suits a question-answering bot for a specific domain, while tool-based orchestration suits an AI assistant that handles many kinds of tasks.

## Where this fits in the 4-tier architecture

RAG belongs to the T3 Capability tier of the 4-tier architecture, because it turns the embedding model and vector database set up in T4 Foundation into a knowledge search capability that an agent can use. In this article, that capability was attached to `ChatClient` as an advisor, which applied the same pipeline to every request. To make search a capability the model calls when it needs to, you have to connect it as a tool, as described above. The next article, [Designing Tool Calling: How LLMs Connect to the Real World](../part4/09-tool-calling-design-in-spring-ai.md), covers how an LLM chooses a tool and how Spring AI runs it and returns the result.

## Hands-on project for this chapter

!!! example "3.8 RAG AI Chatbot CLI Project"
    A document-based RAG chatbot that brings together the flow of Chapter 3. On startup, an offline pipeline runs first: it reads text, JSON, Markdown, and HTML documents from `src/main/resources/data`, unifies their metadata, masks sensitive information, splits them into chunks, and loads them into `SimpleVectorStore`. Then, for each question, it shows the retrieved documents and their scores and streams an answer through this article's Advanced RAG pipeline. For the full run-through, follow the README (in Korean) in [`chapter3/`](https://github.com/JM-Lab/spring-ai-agent-book/tree/main/chapter3) of the example repository.

    ```bash
    ollama pull qwen3.5:4b
    ollama pull bge-m3
    cd chapter3
    ./mvnw spring-boot:run
    # Run a single step: ch3-step1 to ch3-step6
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.ai.cli.step=ch3-step5"
    ```

## More in the book

!!! book "Book sections 3.7-3.8"
    - The structure of the default `QuestionAnswerAdvisor` prompt template, and applying a custom Korean template with different delimiters
    - Configuring compression, rewriting, and translation transformers in sequence, and adjusting the temperature of a `ChatClient` dedicated to transformation
    - Configuring `MultiQueryExpander` and `ConcatenationDocumentJoiner` to broaden the search and merge the results
    - Separating data by tenant with a runtime filter on `VectorStoreDocumentRetriever`
    - The content of the default English templates in `ContextualQueryAugmenter` and its customization options
    - The step-by-step implementation and run results of the hands-on project, split into offline ETL and runtime RAG

    [About the book](../book.md){ .md-button } [Buy the book (Korean)](https://wikibook.co.kr/springai-agents/){ .md-button .md-button--primary }

## References

- [Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks](https://arxiv.org/html/2407.21059v1): the paper by Gao et al. that lays out the module structure and orchestration patterns of Modular RAG
