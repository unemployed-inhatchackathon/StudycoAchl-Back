package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.StudyMaterial;
import com.studycoAchl.hackaton.entity.AppUsers;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.repository.StudyMaterialRepository;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final PdfParsingService pdfParsingService;
    private final MaterialSummaryService materialSummaryService;

    @Value("${app.file.pdf-upload-dir:uploads/pdf}")
    private String pdfUploadDir;

    /**
     * PDF 파일 업로드 및 처리
     */
    public Map<String, Object> uploadPdfMaterial(UUID userUuid, UUID subjectUuid,
                                                 MultipartFile file, String title) {
        try {
            log.info("PDF 자료 업로드 시작 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid);

            // 1. 파일 유효성 검증
            validatePdfFile(file);

            // 2. 사용자와 과목 조회
            AppUsers user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userUuid));

            Subject subject = subjectRepository.findById(subjectUuid)
                    .orElseThrow(() -> new RuntimeException("과목을 찾을 수 없습니다: " + subjectUuid));

            // 3. 파일 저장
            String savedFilePath = saveFile(file);

            // 4. StudyMaterial 엔티티 생성
            StudyMaterial material = StudyMaterial.createPdfMaterial(
                    title != null ? title : file.getOriginalFilename(),
                    savedFilePath,
                    file.getSize(),
                    user,
                    subject
            );

            // 5. 페이지 수 설정
            try {
                int pageCount = pdfParsingService.getPageCount(file);
                material.setPageCount(pageCount);
            } catch (Exception e) {
                log.warn("페이지 수 조회 실패", e);
                material.setPageCount(0);
            }

            material.setProcessingStatus(StudyMaterial.ProcessingStatus.EXTRACTING);
            StudyMaterial savedMaterial = studyMaterialRepository.save(material);

            // 6. 비동기 텍스트 추출 및 요약 시작
            processTextExtractionAndSummary(savedMaterial, file);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("materialUuid", savedMaterial.getUuid());
            result.put("fileName", savedMaterial.getFileName());
            result.put("fileSize", savedMaterial.getFileSize());
            result.put("pageCount", savedMaterial.getPageCount());
            result.put("status", savedMaterial.getProcessingStatus().toString());
            result.put("message", "PDF 자료가 업로드되었습니다. 요약 생성이 진행 중입니다.");

            return result;

        } catch (Exception e) {
            log.error("PDF 자료 업로드 실패", e);
            return Map.of(
                    "success", false,
                    "error", "PDF 업로드 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 텍스트 추출 및 요약 처리 (비동기)
     */
    private void processTextExtractionAndSummary(StudyMaterial material, MultipartFile file) {
        try {
            // 7. PDF 텍스트 추출
            String extractedText = pdfParsingService.extractTextFromPdf(file);
            material.setExtractedText(extractedText);
            material.setProcessingStatus(StudyMaterial.ProcessingStatus.SUMMARIZING);
            studyMaterialRepository.save(material);

            // 8. AI 요약 생성
            String subjectName = material.getSubject().getTitle();
            String summary = materialSummaryService.generateSummary(extractedText, subjectName);

            material.updateSummary(summary);
            studyMaterialRepository.save(material);

            log.info("PDF 처리 완료 - materialUuid: {}", material.getUuid());

        } catch (Exception e) {
            log.error("PDF 처리 실패 - materialUuid: {}", material.getUuid(), e);
            material.setProcessingFailed();
            studyMaterialRepository.save(material);
        }
    }

    /**
     * 학습자료 상세 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMaterialDetail(UUID materialUuid) {
        try {
            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            Map<String, Object> detail = new HashMap<>();
            detail.put("materialUuid", material.getUuid());
            detail.put("fileName", material.getFileName());
            detail.put("fileSize", material.getFileSize());
            detail.put("pageCount", material.getPageCount());
            detail.put("status", material.getProcessingStatus().toString());
            detail.put("createdAt", material.getCreatedAt());
            detail.put("updatedAt", material.getUpdatedAt());
            detail.put("subjectTitle", material.getSubject().getTitle());

            // 요약이 완료된 경우만 포함
            if (material.hasSummary()) {
                detail.put("summary", material.getAiSummary());
                detail.put("summaryGeneratedAt", material.getSummaryGeneratedAt());
            }

            // 키워드 추출
            if (material.getExtractedText() != null) {
                try {
                    String keywords = materialSummaryService.extractKeywords(
                            material.getExtractedText(),
                            material.getSubject().getTitle()
                    );
                    detail.put("keywords", keywords);
                } catch (Exception e) {
                    log.warn("키워드 추출 실패", e);
                }
            }

            return detail;

        } catch (Exception e) {
            log.error("학습자료 상세 조회 실패 - materialUuid: {}", materialUuid, e);
            throw new RuntimeException("학습자료 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자별 학습자료 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserMaterials(UUID userUuid) {
        try {
            List<StudyMaterial> materials = studyMaterialRepository.findByAppUsers_UuidOrderByCreatedAtDesc(userUuid);

            return materials.stream()
                    .map(this::toSummaryMap)
                    .toList();

        } catch (Exception e) {
            log.error("사용자 학습자료 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("학습자료 목록 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 과목별 학습자료 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubjectMaterials(UUID userUuid, UUID subjectUuid) {
        try {
            List<StudyMaterial> materials = studyMaterialRepository
                    .findByAppUsers_UuidAndSubject_Uuid(userUuid, subjectUuid);

            return materials.stream()
                    .map(this::toSummaryMap)
                    .toList();

        } catch (Exception e) {
            log.error("과목별 학습자료 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            throw new RuntimeException("과목별 학습자료 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 요약 재생성
     */
    public Map<String, Object> regenerateSummary(UUID materialUuid) {
        try {
            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            if (material.getExtractedText() == null || material.getExtractedText().isEmpty()) {
                return Map.of(
                        "success", false,
                        "error", "원본 텍스트가 없어 요약을 생성할 수 없습니다."
                );
            }

            // 요약 재생성
            material.setProcessingStatus(StudyMaterial.ProcessingStatus.SUMMARIZING);
            studyMaterialRepository.save(material);

            String newSummary = materialSummaryService.generateSummary(
                    material.getExtractedText(),
                    material.getSubject().getTitle()
            );

            material.updateSummary(newSummary);
            studyMaterialRepository.save(material);

            return Map.of(
                    "success", true,
                    "materialUuid", materialUuid,
                    "summary", newSummary,
                    "message", "요약이 성공적으로 재생성되었습니다."
            );

        } catch (Exception e) {
            log.error("요약 재생성 실패 - materialUuid: {}", materialUuid, e);
            return Map.of(
                    "success", false,
                    "error", "요약 재생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 학습자료 삭제
     */
    public void deleteMaterial(UUID materialUuid) {
        try {
            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            // 파일 삭제
            try {
                Files.deleteIfExists(Paths.get(material.getFilePath()));
                log.info("파일 삭제 완료: {}", material.getFilePath());
            } catch (Exception e) {
                log.warn("파일 삭제 실패: {}", material.getFilePath(), e);
            }

            // DB에서 삭제
            studyMaterialRepository.delete(material);
            log.info("학습자료 삭제 완료 - materialUuid: {}", materialUuid);

        } catch (Exception e) {
            log.error("학습자료 삭제 실패 - materialUuid: {}", materialUuid, e);
            throw new RuntimeException("학습자료 삭제 실패: " + e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * PDF 파일 유효성 검증
     */
    private void validatePdfFile(MultipartFile file) {
        if (!pdfParsingService.isValidPdf(file)) {
            throw new IllegalArgumentException("유효하지 않은 PDF 파일입니다.");
        }

        if (!pdfParsingService.isValidFileSize(file)) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 파일 저장
     */
    private String saveFile(MultipartFile file) throws IOException {
        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(pdfUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유한 파일명 생성
        String originalFileName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
        String filePath = pdfUploadDir + "/" + fileName;

        // 파일 저장
        Path targetPath = Paths.get(filePath);
        file.transferTo(targetPath);

        return filePath;
    }

    /**
     * StudyMaterial을 요약 맵으로 변환
     */
    private Map<String, Object> toSummaryMap(StudyMaterial material) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("materialUuid", material.getUuid());
        summary.put("fileName", material.getFileName());
        summary.put("fileSize", material.getFileSize());
        summary.put("pageCount", material.getPageCount());
        summary.put("status", material.getProcessingStatus().toString());
        summary.put("hasSummary", material.hasSummary());
        summary.put("createdAt", material.getCreatedAt());
        summary.put("subjectTitle", material.getSubject().getTitle());

        if (material.hasSummary()) {
            summary.put("summaryGeneratedAt", material.getSummaryGeneratedAt());
        }

        return summary;
    }

    /**
     * 학습자료 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMaterialStats(UUID userUuid) {
        try {
            long totalMaterials = studyMaterialRepository.findByAppUsers_Uuid(userUuid).size();
            long summarizedMaterials = studyMaterialRepository.countSummarizedMaterialsByUser(userUuid);
            long processingMaterials = studyMaterialRepository
                    .findByProcessingStatus(StudyMaterial.ProcessingStatus.EXTRACTING).size() +
                    studyMaterialRepository
                            .findByProcessingStatus(StudyMaterial.ProcessingStatus.SUMMARIZING).size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMaterials", totalMaterials);
            stats.put("summarizedMaterials", summarizedMaterials);
            stats.put("processingMaterials", processingMaterials);
            stats.put("completionRate", totalMaterials > 0 ?
                    Math.round((double) summarizedMaterials / totalMaterials * 100.0) : 0.0);

            return stats;

        } catch (Exception e) {
            log.error("학습자료 통계 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("통계 조회 실패: " + e.getMessage());
        }
    }
}