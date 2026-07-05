package kr.jmlab.spring.ai.agent.book.chapter5.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DocumentReader / DocumentTransformer 흐름을 MCP 서버 내부에서 재사용한다.
 */
@Component
@Profile("server")
public class RagDocumentLoader {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("\\d{2,3}-\\d{3,4}-\\d{4}");

    private final ObjectMapper objectMapper;

    public RagDocumentLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PreparedDocuments prepare(Path documentsDirectory) {
        List<LoadedDocumentSet> loadedDocumentSets = loadRawDocumentSets(documentsDirectory);
        List<Document> rawDocuments = loadedDocumentSets.stream()
                .flatMap(documentSet -> documentSet.documents().stream())
                .toList();
        List<Document> maskedDocuments = rawDocuments.stream()
                .map(this::maskSensitiveText)
                .toList();
        List<Document> chunks = split(maskedDocuments);

        // 원천 문서, 마스킹 문서, 청크 수를 집계한 보고서를 함께 만들어 반환
        IndexReport report = new IndexReport(
                documentsDirectory.toAbsolutePath().normalize().toString(),
                loadedDocumentSets.stream()
                        .map(documentSet -> new SourceDocument(
                                documentSet.filename(),
                                documentSet.sourceType(),
                                documentSet.documents().size()))
                        .toList(),
                rawDocuments.size(),
                maskedDocuments.size(),
                chunks.size());
        return new PreparedDocuments(report, chunks);
    }

    private List<LoadedDocumentSet> loadRawDocumentSets(Path documentsDirectory) {
        Path directory = documentsDirectory.toAbsolutePath().normalize();
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
        String filename = file.getFileName().toString();
        String sourceType = sourceType(filename);

        List<Document> documents = switch (sourceType) {
            case "text" -> readText(file);
            case "json" -> readBikeCatalog(file);
            case "markdown" -> readMarkdown(file);
            case "html" -> readHtml(file);
            default -> List.of();
        };

        return new LoadedDocumentSet(filename, sourceType, documents.stream()
                .map(document -> withMetadata(document, Map.of(
                        "source", filename,
                        "category", category(sourceType),
                        "sourceType", sourceType,
                        "isActive", true,
                        "ingestedAt", LocalDate.now().toString())))
                .toList());
    }

    private List<Document> readText(Path file) {
        return List.of(document(file.getFileName() + "#text", readString(file), Map.of(
                "docType", "policy",
                "charset", "UTF-8")));
    }

    private List<Document> readBikeCatalog(Path file) {
        try {
            JsonNode bikes = objectMapper.readTree(file.toFile()).at("/store/bikes");
            if (!bikes.isArray()) {
                return List.of();
            }
            return bikes.values().stream()
                    .map(bike -> document(
                            "%s#%s".formatted(file.getFileName(), bike.path("id").asText()),
                            "brand: %s\nmodel: %s\nprice: %s\ndescription: %s".formatted(
                                    bike.path("brand").asText(),
                                    bike.path("model").asText(),
                                    bike.path("price").asText(),
                                    bike.path("description").asText()),
                            Map.of(
                                    "bikeBrand", bike.path("brand").asText(),
                                    "bikeModel", bike.path("model").asText(),
                                    "bikePrice", bike.path("price").asInt(),
                                    "sourceType", "productCatalog")))
                    .toList();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JSON 문서를 읽을 수 없습니다: " + file, ex);
        }
    }

    private List<Document> readMarkdown(Path file) {
        String markdown = readString(file);
        String[] sections = markdown.split("\\n---\\n");
        return Stream.of(sections)
                .map(String::trim)
                .filter(section -> !section.isBlank())
                .map(section -> document(file.getFileName() + "#section-" + Math.abs(section.hashCode()), section, Map.of(
                        "docType", "techGuide",
                        "contentType", "text")))
                .toList();
    }

    private List<Document> readHtml(Path file) {
        try {
            org.jsoup.nodes.Document html = Jsoup.parse(file.toFile(), "UTF-8");
            String text = html.select("article p").eachText().stream()
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse(html.body().text());
            List<String> links = html.select("a[href]").eachAttr("href");

            return List.of(document(file.getFileName() + "#article", text, Map.of(
                    "author", html.selectFirst("meta[name=author]") == null
                            ? "unknown"
                            : html.selectFirst("meta[name=author]").attr("content"),
                    "date", html.selectFirst("meta[name=date]") == null
                            ? "unknown"
                            : html.selectFirst("meta[name=date]").attr("content"),
                    "selector", "article p",
                    "links", links)));
        } catch (IOException ex) {
            throw new IllegalStateException("HTML 문서를 읽을 수 없습니다: " + file, ex);
        }
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

    private Document maskSensitiveText(Document document) {
        String masked = PHONE.matcher(EMAIL.matcher(document.getText()).replaceAll("[EMAIL]")).replaceAll("[PHONE]");
        return Document.builder()
                .id(document.getId())
                .text(masked)
                .metadata(document.getMetadata())
                .build();
    }

    private Document document(String id, String text, Map<String, Object> metadata) {
        return Document.builder()
                .id(id)
                .text(text)
                .metadata(metadata)
                .build();
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

    private String readString(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException ex) {
            throw new IllegalStateException("문서를 읽을 수 없습니다: " + file, ex);
        }
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

    private record LoadedDocumentSet(String filename, String sourceType, List<Document> documents) {
    }

    public record PreparedDocuments(IndexReport report, List<Document> chunks) {
    }

    public record SourceDocument(String filename, String sourceType, int documentCount) {
    }

    public record IndexReport(
            String documentsDirectory,
            List<SourceDocument> sourceDocuments,
            int rawDocumentCount,
            int maskedDocumentCount,
            int chunkCount) {
    }
}
