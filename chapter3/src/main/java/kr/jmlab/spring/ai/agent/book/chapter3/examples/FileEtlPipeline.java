package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * FileDocumentWriter를 활용한 ETL 파이프라인.
 */
@Component
public class FileEtlPipeline {

    /**
     * 외부에서 주입받은 Resource(파일 등)를 읽어 분할한 뒤 파일로 저장
     */
    public EtlResult runPipeline(Resource sourceFile) {
        return runPipeline(sourceFile, Path.of("target", "chapter3-etl-output.txt"));
    }

    public EtlResult runPipeline(Resource sourceFile, Path outputPath) {
        // 1. Extract: 리소스 읽기
        TextReader reader = new TextReader(sourceFile);
        // [중요] TextReader는 페이지 정보가 없으므로, 마커 테스트를 위해 강제로 값을 삽입
        reader.getCustomMetadata().put("page_number", 1);
        reader.getCustomMetadata().put("end_page_number", 1);
        List<Document> originalDocs = reader.read();

        // 2. Transform: 텍스트 분할
        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        List<Document> splitDocs = splitter.apply(originalDocs);

        // 3. Load: 파일로 쓰기
        createParentDirectory(outputPath);
        FileDocumentWriter writer = new FileDocumentWriter(
                outputPath.toString(),
                true,       // withDocumentMarkers: true (마커 포함)
                MetadataMode.ALL,
                false       // append: false (덮어쓰기)
        );
        writer.write(splitDocs);
        return new EtlResult(outputPath, splitDocs.size());
    }

    private void createParentDirectory(Path outputPath) {
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("출력 디렉터리를 만들 수 없습니다: " + outputPath, ex);
        }
    }

    /** 적재 결과(출력 경로와 청크 수)를 담는 레코드 */
    public record EtlResult(Path outputPath, int documentCount) {
    }
}
