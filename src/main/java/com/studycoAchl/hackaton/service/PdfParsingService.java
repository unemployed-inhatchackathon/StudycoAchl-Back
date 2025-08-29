package com.studycoAchl.hackaton.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfParsingService {

    private static final int MAX_TEXT_LENGTH = 50000; // 약 12,500 토큰

    /**
     * PDF 파일에서 전체 텍스트 추출
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            return preprocessText(rawText);
        }
    }

    /**
     * 파일 경로로 텍스트 추출
     */
    public String extractTextFromPdfFile(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new java.io.File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            return preprocessText(rawText);
        }
    }

    /**
     * PDF 페이지 수 조회
     */
    public int getPageCount(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return document.getNumberOfPages();
        }
    }

    /**
     * 파일 경로로 페이지 수 조회
     */
    public int getPageCountFromFile(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new java.io.File(filePath))) {
            return document.getNumberOfPages();
        }
    }

    /**
     * PDF 파일 유효성 검증
     */
    public boolean isValidPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean isValidContentType = "application/pdf".equals(contentType);
        boolean isValidFileName = fileName != null && fileName.toLowerCase().endsWith(".pdf");

        return isValidContentType || isValidFileName;
    }

    /**
     * 파일 크기 검증 (10MB 제한)
     */
    public boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= 10 * 1024 * 1024; // 10MB
    }

    /**
     * 텍스트 전처리
     */
    private String preprocessText(String rawText) {
        if (rawText == null) {
            return "";
        }

        String processed = rawText
                .replaceAll("\\s+", " ")  // 여러 공백을 하나로
                .replaceAll("[\\r\\n]+", "\n")  // 개행 정리
                .replaceAll("\\p{Cntrl}", " ")  // 제어문자 제거
                .trim();

        // 토큰 제한 적용
        if (processed.length() > MAX_TEXT_LENGTH) {
            processed = processed.substring(0, MAX_TEXT_LENGTH) + "...";
        }

        return processed;
    }

    /**
     * PDF 메타데이터 정보 생성
     */
    public String getPdfInfo(MultipartFile file) throws IOException {
        int pageCount = getPageCount(file);
        double fileSizeMB = file.getSize() / 1024.0 / 1024.0;

        return String.format("페이지 수: %d, 파일명: %s, 크기: %.2f MB",
                pageCount, file.getOriginalFilename(), fileSizeMB);
    }
}