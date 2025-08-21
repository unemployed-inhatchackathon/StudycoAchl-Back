package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.dto.CurrentQuestionResponse;
import com.studycoAchl.hackaton.dto.SessionStatusResponse;
import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.studycoAchl.hackaton.service.ProblemSessionService;
import com.studycoAchl.hackaton.service.ProblemGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/problem-session")
@RequiredArgsConstructor
@Slf4j
public class ProblemSessionController {

    private final ProblemSessionService sessionService;
    private final ProblemGenerationService problemGenerationService;
    private final ProblemRepository problemRepository;

    /**
     * 채팅 세션 기반 AI 문제 생성 및 세션 시작
     */
    @PostMapping("/start-from-chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startFromChat(
            @RequestParam UUID userId,
            @RequestParam UUID chatSessionId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(defaultValue = "5") int questionCount) {

        try {
            log.info("채팅 기반 AI 문제 생성 요청 - userId: {}, chatSessionId: {}, count: {}",
                    userId, chatSessionId, questionCount);

            if (subjectId == null) {
                return ResponseEntity.ok(ApiResponse.error("과목 ID는 필수입니다."));
            }

            Map<String, Object> result = problemGenerationService.generateProblemsFromChatSession(
                    userId, subjectId, chatSessionId, questionCount);

            if (!(boolean) result.get("success")) {
                log.error("문제 생성 실패: {}", result.get("error"));
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

            log.info("AI 문제 생성 성공 - problemId: {}", result.get("problemUuid"));

            return ResponseEntity.ok(ApiResponse.success(result,
                    "AI가 채팅 내용을 분석해서 맞춤 문제를 생성했습니다!"));

        } catch (Exception e) {
            log.error("채팅 기반 문제 생성 실패", e);
            return ResponseEntity.ok(ApiResponse.error("AI 문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 키워드 직접 입력으로 AI 문제 생성
     */
    @PostMapping("/generate-from-keywords")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateFromKeywords(
            @RequestParam UUID userId,
            @RequestParam UUID subjectId,
            @RequestParam String keywords,
            @RequestParam(defaultValue = "학습자가 입력한 키워드를 바탕으로 한 문제") String context,
            @RequestParam(defaultValue = "5") int questionCount) {

        try {
            log.info("키워드 기반 AI 문제 생성 요청 - keywords: {}, count: {}", keywords, questionCount);

            String[] keywordArray = keywords.split(",");
            for (int i = 0; i < keywordArray.length; i++) {
                keywordArray[i] = keywordArray[i].trim();
            }

            Map<String, Object> result = problemGenerationService.generateProblemsFromKeywords(
                    userId, subjectId, Arrays.asList(keywordArray), context, questionCount);

            if (!(boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("error")));
            }

            return ResponseEntity.ok(ApiResponse.success(result,
                    "키워드 기반으로 AI 문제가 생성되었습니다!"));

        } catch (Exception e) {
            log.error("키워드 기반 문제 생성 실패", e);
            return ResponseEntity.ok(ApiResponse.error("키워드 기반 문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 생성된 문제 내용 조회
     */
    @GetMapping("/problem/{problemUuid}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProblemContent(
            @PathVariable UUID problemUuid) {

        try {
            log.info("문제 내용 조회 요청 - problemUuid: {}", problemUuid);

            Optional<Problem> problemOpt = problemRepository.findById(problemUuid);
            if (problemOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("문제를 찾을 수 없습니다."));
            }

            Problem problem = problemOpt.get();
            String problemsJson = problem.getProblems();

            // JSON을 Map으로 파싱
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> problemData = mapper.readValue(problemsJson, Map.class);

            return ResponseEntity.ok(ApiResponse.success(problemData, "문제 내용을 성공적으로 가져왔습니다."));

        } catch (Exception e) {
            log.error("문제 내용 조회 실패 - problemUuid: {}", problemUuid, e);
            return ResponseEntity.ok(ApiResponse.error("문제 내용 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 세션의 현재 문제 조회
     */
    @GetMapping("/{sessionId}/current")
    public ResponseEntity<ApiResponse<CurrentQuestionResponse>> getCurrentQuestion(
            @PathVariable UUID sessionId) {

        try {
            log.info("현재 문제 조회 요청 - sessionId: {}", sessionId);

            CurrentQuestionResponse response = sessionService.getCurrentQuestion(sessionId);

            log.info("현재 문제 조회 성공 - questionId: {}", response.getQuestionId());

            return ResponseEntity.ok(ApiResponse.success(response, "현재 문제를 성공적으로 가져왔습니다."));

        } catch (Exception e) {
            log.error("현재 문제 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.ok(ApiResponse.error("문제 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 세션 상태 조회
     */
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<ApiResponse<SessionStatusResponse>> getSessionStatus(
            @PathVariable UUID sessionId) {

        try {
            log.info("세션 상태 조회 요청 - sessionId: {}", sessionId);

            SessionStatusResponse response = sessionService.getSessionStatus(sessionId);

            return ResponseEntity.ok(ApiResponse.success(response, "세션 상태를 성공적으로 가져왔습니다."));

        } catch (Exception e) {
            log.error("세션 상태 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.ok(ApiResponse.error("세션 상태 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 다음 문제로 이동
     */
    @PostMapping("/{sessionId}/next")
    public ResponseEntity<ApiResponse<Map<String, Object>>> nextQuestion(
            @PathVariable UUID sessionId) {

        try {
            log.info("다음 문제 요청 - sessionId: {}", sessionId);

            Map<String, Object> result = sessionService.moveToNextQuestion(sessionId);

            return ResponseEntity.ok(ApiResponse.success(result, "다음 문제로 이동했습니다."));

        } catch (Exception e) {
            log.error("다음 문제 이동 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.ok(ApiResponse.error("다음 문제 이동 실패: " + e.getMessage()));
        }
    }

    /**
     * 답안 제출 및 채점
     */
    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            @PathVariable UUID sessionId,
            @RequestParam int questionNumber,
            @RequestParam int selectedAnswer,
            @RequestParam(required = false) UUID userId) {

        try {
            log.info("답안 제출 - sessionId: {}, questionNumber: {}, selectedAnswer: {}",
                    sessionId, questionNumber, selectedAnswer);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "sessionId", sessionId.toString(),
                    "questionNumber", questionNumber,
                    "selectedAnswer", selectedAnswer,
                    "message", "답안이 제출되었습니다. (채점 로직 구현 필요)"
            );

            return ResponseEntity.ok(ApiResponse.success(result, "답안이 성공적으로 제출되었습니다."));

        } catch (Exception e) {
            log.error("답안 제출 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.ok(ApiResponse.error("답안 제출 실패: " + e.getMessage()));
        }
    }

    /**
     * 문제 세트 전체 조회
     */
    @GetMapping("/{sessionId}/all-questions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllQuestions(
            @PathVariable UUID sessionId) {

        try {
            log.info("전체 문제 조회 요청 - sessionId: {}", sessionId);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "sessionId", sessionId.toString(),
                    "message", "전체 문제 조회 기능 구현 필요"
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("전체 문제 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.ok(ApiResponse.error("전체 문제 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 시스템 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            Map<String, Object> status = Map.of(
                    "status", "정상",
                    "service", "AI 문제풀이 시스템",
                    "features", Arrays.asList(
                            "채팅 기반 AI 문제 생성",
                            "키워드 기반 문제 생성",
                            "실시간 문제 조회",
                            "답안 제출 및 채점"
                    ),
                    "aiModel", "OpenAI GPT-3.5-turbo"
            );

            return ResponseEntity.ok(ApiResponse.success(status, "AI 문제풀이 시스템 정상 작동!"));

        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("시스템 상태 확인 실패: " + e.getMessage()));
        }
    }

    /**
     * OpenAI 연결 테스트
     */
    @GetMapping("/test-ai")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testAI() {
        try {
            log.info("OpenAI 연결 테스트 시작");

            // 테스트용 UUID 생성
            UUID testUserId = UUID.randomUUID();
            UUID testSubjectId = UUID.randomUUID();

            Map<String, Object> result = problemGenerationService.generateProblemsFromKeywords(
                    testUserId, testSubjectId,
                    Arrays.asList("테스트"), "OpenAI 연결 테스트", 1);

            if ((boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponse.success(result, "OpenAI 연결 성공!"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("OpenAI 연결 실패: " + result.get("error")));
            }

        } catch (Exception e) {
            log.error("OpenAI 테스트 실패", e);
            return ResponseEntity.ok(ApiResponse.error("OpenAI 테스트 실패: " + e.getMessage()));
        }
    }
}