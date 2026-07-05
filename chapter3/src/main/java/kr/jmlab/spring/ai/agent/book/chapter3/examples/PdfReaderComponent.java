package kr.jmlab.spring.ai.agent.book.chapter3.examples;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * PDF 읽기 컴포넌트.
 *
 * <p>Spring AI 내장 PDF 리더(PagePdfDocumentReader / ParagraphPdfDocumentReader)로
 * 페이지 단위와 문단 단위 추출 방식을 비교한다.</p>
 */
@Component
public class PdfReaderComponent {

    /**
     * 페이지 단위로 PDF 읽기
     * - PagePdfDocumentReader를 사용하여 각 페이지를 독립된 Document로 생성
     */
    public List<Document> readByPage(Resource pdfResource) {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                pdfResource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)    // 상단 여백 제외
                        .withPageBottomMargin(0) // 하단 여백 제외
                        .withPagesPerDocument(1) // 1페이지당 1개의 Document 생성
                        .build()
        );

        return pdfReader.read();
    }

    /**
     * 문단 단위로 PDF 읽기
     * - ParagraphPdfDocumentReader를 사용하여 텍스트 흐름을 유지하며 읽기
     * - 문장이 페이지 넘김으로 인해 끊기는 것을 방지
     */
    public List<Document> readByParagraph(Resource pdfResource) {
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(
                pdfResource,
                PdfDocumentReaderConfig.builder()
                        // 머리글 텍스트 제거를 위한 포매터 설정
                        .withPageExtractedTextFormatter(
                                ExtractedTextFormatter.builder()
                                        .withNumberOfTopTextLinesToDelete(1)
                                        .build())
                        .build()
        );

        return pdfReader.read();
    }
}
