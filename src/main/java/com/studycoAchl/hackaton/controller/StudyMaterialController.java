package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.service.StudyMaterialService;
import com.studycoAchl.hackaton.service.PdfQuizGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/study-materials")
@RequiredArgsConstructor
@Slf4j
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;
    private final PdfQuizGenerationService pdfQuizGenerationService;

    /**
     * PDF 학습자료 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPdfMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userUuid") UUID userUuid,
            @RequestParam("subjectUuid") UUID subjectUuid,
            @RequestParam(value = "title", required = false) String title) {

        try {
            log.info("PDF 학습자료 업로드 요청 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid);

            Map<String, Object> result = studyMaterialService.uploadPdfMaterial(userUuid, subjectUuid, file, title);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(result, "PDF 학습자료가 업로드되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("PDF 업로드 실패", e);
            return ResponseEntity.ok(ApiResponse.error("PDF 업로드 실패: " + e.getMessage()));
        }
    }

    /**
     * 학습자료 상세 조회 (요약 포함)
     */
    @GetMapping("/{materialUuid}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaterialDetail(
            @PathVariable UUID materialUuid) {

        try {
            Map<String, Object> detail = studyMaterialService.getMaterialDetail(materialUuid);
            return ResponseEntity.ok(ApiResponse.success(detail, "학습자료 상세 정보를 조회했습니다."));

        } catch (Exception e) {
            log.error("학습자료 상세 조회 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("학습자료 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 학습자료 목록 조회
     */
    @GetMapping("/users/{userUuid}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserMaterials(
            @PathVariable UUID userUuid) {

        try {
            List<Map<String, Object>> materials = studyMaterialService.getUserMaterials(userUuid);
            return ResponseEntity.ok(ApiResponse.success(materials, "학습자료 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("사용자 학습자료 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("학습자료 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 과목별 학습자료 조회
     */
    @GetMapping("/users/{userUuid}/subjects/{subjectUuid}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubjectMaterials(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid) {

        try {
            List<Map<String, Object>> materials = studyMaterialService.getSubjectMaterials(userUuid, subjectUuid);
            return ResponseEntity.ok(ApiResponse.success(materials, "과목별 학습자료를 조회했습니다."));

        } catch (Exception e) {
            log.error("과목별 학습자료 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목별 학습자료 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 요약 재생성
     */
    @PostMapping("/{materialUuid}/regenerate-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateSummary(
            @PathVariable UUID materialUuid) {

        try {
            Map<String, Object> result = studyMaterialService.regenerateSummary(materialUuid);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "요약이 재생성되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("요약 재생성 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("요약 재생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 학습자료 삭제
     */
    @DeleteMapping("/{materialUuid}")
    public ResponseEntity<ApiResponse<String>> deleteMaterial(@PathVariable UUID materialUuid) {
        try {
            studyMaterialService.deleteMaterial(materialUuid);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "학습자료가 삭제되었습니다."));

        } catch (Exception e) {
            log.error("학습자료 삭제 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("학습자료 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * PDF 요약 기반 문제 생성
     */
    @PostMapping("/{materialUuid}/generate-quiz-from-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateQuizFromSummary(
            @PathVariable UUID materialUuid,
            @RequestParam(defaultValue = "5") int questionCount) {

        try {
            Map<String, Object> result = pdfQuizGenerationService.generateQuizFromSummary(materialUuid, questionCount);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "요약 기반 문제가 생성되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("요약 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * PDF 전체 텍스트 기반 문제 생성
     */
    @PostMapping("/{materialUuid}/generate-quiz-from-text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateQuizFromFullText(
            @PathVariable UUID materialUuid,
            @RequestParam(defaultValue = "5") int questionCount,
            @RequestParam(defaultValue = "보통") String difficulty) {

        try {
            Map<String, Object> result = pdfQuizGenerationService.generateQuizFromFullText(
                    materialUuid, questionCount, difficulty);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "전체 텍스트 기반 문제가 생성되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("전체 텍스트 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 키워드 기반 문제 생성
     */
    @PostMapping("/{materialUuid}/generate-quiz-from-keywords")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateQuizFromKeywords(
            @PathVariable UUID materialUuid,
            @RequestParam String keywords,
            @RequestParam(defaultValue = "5") int questionCount,
            @RequestParam(defaultValue = "보통") String difficulty) {

        try {
            Map<String, Object> result = pdfQuizGenerationService.generateQuizFromKeywords(
                    materialUuid, keywords, questionCount, difficulty);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "키워드 기반 문제가 생성되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("키워드 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 학습자료별 퀴즈 목록 조회
     */
    @GetMapping("/{materialUuid}/quizzes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMaterialQuizzes(
            @PathVariable UUID materialUuid) {

        try {
            List<Map<String, Object>> quizzes = pdfQuizGenerationService.getMaterialQuizzes(materialUuid);
            return ResponseEntity.ok(ApiResponse.success(quizzes, "학습자료 퀴즈 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("학습자료 퀴즈 조회 실패 - materialUuid: {}", materialUuid, e);
            return ResponseEntity.ok(ApiResponse.error("퀴즈 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 학습자료 통계 조회
     */
    @GetMapping("/users/{userUuid}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaterialStats(
            @PathVariable UUID userUuid) {

        try {
            Map<String, Object> stats = studyMaterialService.getMaterialStats(userUuid);
            return ResponseEntity.ok(ApiResponse.success(stats, "학습자료 통계를 조회했습니다."));

        } catch (Exception e) {
            log.error("학습자료 통계 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("통계 조회 실패: " + e.getMessage()));
        }
    }
}