package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.service.PdfQuizGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/material-quizzes")
@RequiredArgsConstructor
@Slf4j
public class MaterialQuizController {

    private final PdfQuizGenerationService pdfQuizGenerationService;

    /**
     * 퀴즈 상세 조회 (문제 데이터 포함)
     */
    @GetMapping("/{quizUuid}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizDetail(
            @PathVariable UUID quizUuid) {

        try {
            Map<String, Object> detail = pdfQuizGenerationService.getQuizDetail(quizUuid);
            return ResponseEntity.ok(ApiResponse.success(detail, "퀴즈 상세 정보를 조회했습니다."));

        } catch (Exception e) {
            log.error("퀴즈 상세 조회 실패 - quizUuid: {}", quizUuid, e);
            return ResponseEntity.ok(ApiResponse.error("퀴즈 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 PDF 퀴즈 목록 조회
     */
    @GetMapping("/users/{userUuid}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserPdfQuizzes(
            @PathVariable UUID userUuid) {

        try {
            List<Map<String, Object>> quizzes = pdfQuizGenerationService.getUserPdfQuizzes(userUuid);
            return ResponseEntity.ok(ApiResponse.success(quizzes, "PDF 퀴즈 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("사용자 PDF 퀴즈 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("PDF 퀴즈 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 퀴즈 삭제
     */
    @DeleteMapping("/{quizUuid}")
    public ResponseEntity<ApiResponse<String>> deleteQuiz(@PathVariable UUID quizUuid) {
        try {
            pdfQuizGenerationService.deleteQuiz(quizUuid);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "퀴즈가 삭제되었습니다."));

        } catch (Exception e) {
            log.error("퀴즈 삭제 실패 - quizUuid: {}", quizUuid, e);
            return ResponseEntity.ok(ApiResponse.error("퀴즈 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * PDF 퀴즈 통계 조회
     */
    @GetMapping("/users/{userUuid}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPdfQuizStats(
            @PathVariable UUID userUuid) {

        try {
            Map<String, Object> stats = pdfQuizGenerationService.getPdfQuizStats(userUuid);
            return ResponseEntity.ok(ApiResponse.success(stats, "PDF 퀴즈 통계를 조회했습니다."));

        } catch (Exception e) {
            log.error("PDF 퀴즈 통계 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("통계 조회 실패: " + e.getMessage()));
        }
    }
}