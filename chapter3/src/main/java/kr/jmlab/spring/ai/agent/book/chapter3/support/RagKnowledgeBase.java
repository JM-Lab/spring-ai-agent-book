package kr.jmlab.spring.ai.agent.book.chapter3.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import kr.jmlab.spring.ai.agent.book.chapter3.examples.JsonDocReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.MarkdownDocsReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.PiiMaskingTransformer;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.SimpleTextReader;
import kr.jmlab.spring.ai.agent.book.chapter3.examples.WebPageReader;

/**
 * 3장 CLI 프로젝트가 공통으로 사용하는 작은 지식 베이스.
 *
 * <p>ETL 파이프라인과 벡터 저장소의 흐름을 그대로 반영한다.
 * 여러 DocumentReader로 원천 문서를 수집하고, 메타데이터를 통일한 뒤,
 * 민감 정보를 마스킹하고, 검색 단위로 청킹하여 VectorStore에 적재한다.</p>
 */
@Component
public class RagKnowledgeBase {

    private final PiiMaskingTransformer piiMaskingTransformer;
    private final VectorStore vectorStore;
    private final Path documentsDirectory;
    private boolean indexed;
    private List<Document> indexedDocuments = List.of();
    private IndexReport lastIndexReport;

    public RagKnowledgeBase(
            @Value("${spring.ai.rag.documents-dir:src/main/resources/data}") String documentsDirectory,
            PiiMaskingTransformer piiMaskingTransformer,
            VectorStore vectorStore) {
        this.documentsDirectory = Path.of(documentsDirectory);
        this.piiMaskingTransformer = piiMaskingTransformer;
        this.vectorStore = vectorStore;
    }

    public synchronized List<Document> indexIfNecessary() {
        if (!indexed) {
            indexOffline();
        }
        return indexedDocuments;
    }

    public synchronized IndexReport indexOffline() {
        if (indexed && lastIndexReport != null) {
            return lastIndexReport;
        }

        List<LoadedDocumentSet> loadedDocumentSets = loadRawDocumentSets();
        List<Document> rawDocuments = loadedDocumentSets.stream()
                .flatMap(documentSet -> documentSet.documents().stream())
                .toList();

        List<Document> maskedDocuments = piiMaskingTransformer.transform(rawDocuments);
        List<Document> chunks = split(maskedDocuments);

        if (chunks.isEmpty()) {
            throw new IllegalStateException("인덱싱할 문서가 없습니다: " + documentsDirectory());
        }

        vectorStore.add(chunks);
        indexed = true;
        indexedDocuments = List.copyOf(chunks);
        lastIndexReport = new IndexReport(
                documentsDirectory(),
                loadedDocumentSets.stream()
                        .map(documentSet -> new SourceDocument(
                                documentSet.filename(),
                                documentSet.sourceType(),
                                documentSet.documents().size()))
                        .toList(),
                rawDocuments.size(),
                maskedDocuments.size(),
                chunks.size()
        );
        return lastIndexReport;
    }

    public List<Document> loadRawDocuments() {
        return loadRawDocumentSets().stream()
                .flatMap(documentSet -> documentSet.documents().stream())
                .toList();
    }

    public List<Document> loadDocuments() {
        List<Document> maskedDocuments = piiMaskingTransformer.transform(loadRawDocuments());
        return split(maskedDocuments);
    }

    public Path documentsDirectory() {
        return documentsDirectory.toAbsolutePath().normalize();
    }

    private List<LoadedDocumentSet> loadRawDocumentSets() {
        Path directory = documentsDirectory();
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("문서 디렉터리를 찾을 수 없습니다: " + directory);
        }

        try (Stream<Path> files = Files.list(directory)) {
            List<LoadedDocumentSet> documentSets = files
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedDocument)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::readDocumentFile)
                    .filter(documentSet -> !documentSet.documents().isEmpty())
                    .toList();

            if (documentSets.isEmpty()) {
                throw new IllegalStateException("지원하는 문서 파일이 없습니다: " + directory);
            }

            return documentSets;
        } catch (IOException ex) {
            throw new IllegalStateException("문서 디렉터리를 읽을 수 없습니다: " + directory, ex);
        }
    }

    private LoadedDocumentSet readDocumentFile(Path file) {
        Resource resource = new FileSystemResource(file);
        String filename = file.getFileName().toString();
        // 디렉터리 안의 각 파일은 확장자에 따라 적절한 DocumentReader 구현체로 연결
        String sourceType = sourceType(filename);

        List<Document> documents = switch (sourceType) {
            case "text" -> new SimpleTextReader(resource).readWithMetadata();
            case "json" -> new JsonDocReader(resource).readBikesWithMetadata();
            case "markdown" -> new MarkdownDocsReader(resource).loadMarkdown();
            case "html" -> new WebPageReader(resource).readArticleContent();
            default -> List.of();
        };

        return new LoadedDocumentSet(filename, sourceType, documents.stream()
                .map(document -> withMetadata(document, Map.of(
                        "source", filename,
                        "category", category(sourceType),
                        "sourceType", sourceType,
                        "isActive", true,
                        "ingestedAt", LocalDate.now().toString()
                )))
                .toList());
    }

    private boolean isSupportedDocument(Path file) {
        String filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".txt")
                || filename.endsWith(".json")
                || filename.endsWith(".md")
                || filename.endsWith(".html")
                || filename.endsWith(".htm");
    }

    private String sourceType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".txt")) {
            return "text";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        if (lower.endsWith(".md")) {
            return "markdown";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "html";
        }
        return "unknown";
    }

    private String category(String sourceType) {
        return switch (sourceType) {
            case "json" -> "product_catalog";
            case "html" -> "web_docs";
            default -> "tech_docs";
        };
    }

    private List<Document> split(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(300)
                .withMinChunkSizeChars(80)
                .withMinChunkLengthToEmbed(20)
                .withMaxNumChunks(100)
                .withKeepSeparator(true)
                .build();

        return splitter.split(documents);
    }

    private Document withMetadata(Document document, Map<String, Object> metadata) {
        Map<String, Object> mergedMetadata = new LinkedHashMap<>(document.getMetadata());
        mergedMetadata.putAll(metadata);

        return Document.builder()
                .id(document.getId())
                .text(document.getText())
                .metadata(mergedMetadata)
                .build();
    }

    private record LoadedDocumentSet(String filename, String sourceType, List<Document> documents) {
    }

    public record SourceDocument(String filename, String sourceType, int documentCount) {
    }

    public record IndexReport(
            Path documentsDirectory,
            List<SourceDocument> sourceDocuments,
            int rawDocumentCount,
            int maskedDocumentCount,
            int chunkCount) {
    }
}
