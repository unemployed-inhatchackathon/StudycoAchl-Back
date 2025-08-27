package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.entity.QuizResult;
import com.studycoAchl.hackaton.service.GradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
@Slf4j
public class GradingController {

    private final GradingService gradingService;

    /**
     * 답안 제출 및 채점
     */
    @PostMapping("/problems/{problemUuid}/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            @PathVariable UUID problemUuid,
            @RequestParam int questionNumber,
            @RequestParam int selectedAnswer,
            @RequestParam UUID userUuid) {

        try {
            log.info("답안 제출 요청 - problemUuid: {}, questionNumber: {}", problemUuid, questionNumber);

            Map<String, Object> result = gradingService.submitAnswer(problemUuid, questionNumber, selectedAnswer, userUuid);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "답안이 제출되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("답안 제출 실패 - problemUuid: {}", problemUuid, e);
            return ResponseEntity.ok(ApiResponse.error("답안 제출 실패: " + e.getMessage()));
        }
    }

    /**
     * 퀴즈 완료 및 최종 채점
     */
    @PostMapping("/problems/{problemUuid}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeQuiz(
            @PathVariable UUID problemUuid) {

        try {
            log.info("퀴즈 완료 요청 - problemUuid: {}", problemUuid);

            Map<String, Object> result = gradingService.completeQuiz(problemUuid);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "퀴즈가 완료되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("퀴즈 완료 실패 - problemUuid: {}", problemUuid, e);
            return ResponseEntity.ok(ApiResponse.error("퀴즈 완료 실패: " + e.getMessage()));
        }
    }

    /**
     * 퀴즈 결과 조회
     */
    @GetMapping("/problems/{problemUuid}/result")
    public ResponseEntity<ApiResponse<QuizResult>> getQuizResult(
            @PathVariable UUID problemUuid) {

        try {
            QuizResult result = gradingService.getQuizResult(problemUuid);
            return ResponseEntity.ok(ApiResponse.success(result, "퀴즈 결과를 조회했습니다."));

        } catch (Exception e) {
            log.error("퀴즈 결과 조회 실패 - problemUuid: {}", problemUuid, e);
            return ResponseEntity.ok(ApiResponse.error("퀴즈 결과 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 사용자 학습 통계
     */
    @GetMapping("/users/{userUuid}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(
            @PathVariable UUID userUuid) {

        try {
            Map<String, Object> stats = gradingService.getUserStats(userUuid);
            return ResponseEntity.ok(ApiResponse.success(stats, "학습 통계를 조회했습니다."));

        } catch (Exception e) {
            log.error("학습 통계 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("학습 통계 조회 실패: " + e.getMessage()));
        }
    }
}