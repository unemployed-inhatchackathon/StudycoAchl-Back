package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.service.WrongAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wrong-answers")
@RequiredArgsConstructor
@Slf4j
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    /**
     * 사용자 전체 오답노트 조회
     */
    @GetMapping("/users/{userUuid}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserWrongAnswers(
            @PathVariable UUID userUuid) {

        try {
            log.info("사용자 오답노트 조회 요청 - userUuid: {}", userUuid);

            Map<String, Object> result = wrongAnswerService.getUserWrongAnswers(userUuid);
            return ResponseEntity.ok(ApiResponse.success(result, "오답노트를 조회했습니다."));

        } catch (Exception e) {
            log.error("오답노트 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("오답노트 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 과목별 오답노트 조회
     */
    @GetMapping("/users/{userUuid}/subjects/{subjectUuid}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubjectWrongAnswers(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid) {

        try {
            log.info("과목별 오답노트 조회 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid);

            Map<String, Object> result = wrongAnswerService.getSubjectWrongAnswers(userUuid, subjectUuid);
            return ResponseEntity.ok(ApiResponse.success(result, "과목별 오답노트를 조회했습니다."));

        } catch (Exception e) {
            log.error("과목별 오답노트 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목별 오답노트 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 복습 퀴즈 생성 (틀린 문제만으로)
     */
    @PostMapping("/users/{userUuid}/subjects/{subjectUuid}/review-quiz")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReviewQuiz(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid,
            @RequestParam(defaultValue = "10") int maxQuestions) {

        try {
            log.info("복습 퀴즈 생성 요청 - userUuid: {}, subjectUuid: {}, maxQuestions: {}",
                    userUuid, subjectUuid, maxQuestions);

            Map<String, Object> result = wrongAnswerService.generateReviewQuiz(userUuid, subjectUuid, maxQuestions);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "복습 퀴즈가 생성되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("message")));
            }

        } catch (Exception e) {
            log.error("복습 퀴즈 생성 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("복습 퀴즈 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 오답 복습 완료 처리
     */
    @PostMapping("/{wrongAnswerNoteUuid}/review-complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markReviewCompleted(
            @PathVariable UUID wrongAnswerNoteUuid,
            @RequestParam boolean isCorrect) {

        try {
            log.info("복습 완료 처리 - wrongAnswerNoteUuid: {}, isCorrect: {}", wrongAnswerNoteUuid, isCorrect);

            Map<String, Object> result = wrongAnswerService.markReviewCompleted(wrongAnswerNoteUuid, isCorrect);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "복습이 완료되었습니다."));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

        } catch (Exception e) {
            log.error("복습 완료 처리 실패 - wrongAnswerNoteUuid: {}", wrongAnswerNoteUuid, e);
            return ResponseEntity.ok(ApiResponse.error("복습 완료 처리 실패: " + e.getMessage()));
        }
    }

    /**
     * 오답노트 통계 (키워드별 약점 분석)
     */
    @GetMapping("/users/{userUuid}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWrongAnswerStats(
            @PathVariable UUID userUuid,
            @RequestParam(required = false) UUID subjectUuid) {

        try {
            log.info("오답노트 통계 조회 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid);

            Map<String, Object> stats;
            if (subjectUuid != null) {
                stats = wrongAnswerService.getSubjectWrongAnswers(userUuid, subjectUuid);
            } else {
                stats = wrongAnswerService.getUserWrongAnswers(userUuid);
            }

            return ResponseEntity.ok(ApiResponse.success(stats, "오답노트 통계를 조회했습니다."));

        } catch (Exception e) {
            log.error("오답노트 통계 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("오답노트 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 오답노트 개별 삭제 (숙지 완료)
     */
    @DeleteMapping("/{wrongAnswerNoteUuid}")
    public ResponseEntity<ApiResponse<String>> deleteWrongAnswerNote(
            @PathVariable UUID wrongAnswerNoteUuid) {

        try {
            log.info("오답노트 삭제 - wrongAnswerNoteUuid: {}", wrongAnswerNoteUuid);

            Map<String, Object> result = wrongAnswerService.markReviewCompleted(wrongAnswerNoteUuid, true);

            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "오답노트에서 제거되었습니다."));

        } catch (Exception e) {
            log.error("오답노트 삭제 실패 - wrongAnswerNoteUuid: {}", wrongAnswerNoteUuid, e);
            return ResponseEntity.ok(ApiResponse.error("오답노트 삭제 실패: " + e.getMessage()));
        }
    }
}