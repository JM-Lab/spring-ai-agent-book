package kr.jmlab.spring.ai.agent.book.chapter3;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.JsonDocReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.MarkdownDocsReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.SimpleTextReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.WebPageReader;

/**
 * 3장 Step 1: DocumentReader 로 원천 문서 읽기.
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.cli", name = "step", havingValue = "ch3-step1")
class Ch3Step1_DocumentReaders implements CommandLineRunner {

    private final SimpleTextReader simpleTextReader;
    private final JsonDocReader jsonDocReader;
    private final MarkdownDocsReader markdownDocsReader;
    private final WebPageReader webPageReader;

    Ch3Step1_DocumentReaders(
            SimpleTextReader simpleTextReader,
            JsonDocReader jsonDocReader,
            MarkdownDocsReader markdownDocsReader,
            WebPageReader webPageReader) {
        this.simpleTextReader = simpleTextReader;
        this.jsonDocReader = jsonDocReader;
        this.markdownDocsReader = markdownDocsReader;
        this.webPageReader = webPageReader;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== [Ch3] Step1: DocumentReader ===");

        List<Document> textDocs = simpleTextReader.readWithMetadata();
        printPreview("TextReader", textDocs);

        List<Document> bikeDocs = jsonDocReader.readBikesWithMetadata();
        printPreview("JsonReader", bikeDocs);

        List<Document> markdownDocs = markdownDocsReader.loadMarkdown();
        printPreview("MarkdownDocsReader", markdownDocs);

        List<Document> htmlDocs = webPageReader.readArticleContent();
        printPreview("WebPageReader", htmlDocs);
    }

    private void printPreview(String title, List<Document> documents) {
        System.out.printf("%n[%s] document count: %d%n", title, documents.size());
        documents.stream().limit(2).forEach(doc -> {
            String text = doc.getText().replaceAll("\\s+", " ");
            System.out.println("- " + text.substring(0, Math.min(text.length(), 120)));
            System.out.println("  metadata: " + doc.getMetadata());
        });
    }
}
