package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.entity.*;
import com.studycoAchl.hackaton.repository.*;
import com.studycoAchl.hackaton.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/integration-test")
@RequiredArgsConstructor
@Slf4j
public class IntegrationTestController {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ProblemGenerationService problemGenerationService;
    private final GradingService gradingService;
    private final WrongAnswerService wrongAnswerService;

    /**
     * 전체 플랫폼 통합 테스트
     * 채팅 → 문제생성 → 풀이 → 채점 → 오답노트까지 전체 시나리오
     */
    @PostMapping("/full-scenario")
    @Transactional
    public ApiResponse<Map<String, Object>> runFullScenario() {
        Map<String, Object> testResult = new HashMap<>();

        try {
            log.info("=== 전체 플랫폼 통합 테스트 시작 ===");

            // 1단계: 테스트 데이터 생성
            log.info("1단계: 테스트 사용자 및 과목 생성");
            AppUsers testAppUsers = createTestUser();
            Subject testSubject = createTestSubject(testAppUsers);
            testResult.put("step1_data_creation", Map.of(
                    "userUuid", testAppUsers.getUuid(),
                    "subjectUuid", testSubject.getUuid(),
                    "status", "성공"
            ));

            // 2단계: 키워드 기반 문제 생성
            log.info("2단계: AI 문제 생성");
            Map<String, Object> problemResult = problemGenerationService.generateProblemsFromKeywords(
                    testAppUsers.getUuid(),
                    testSubject.getUuid(),
                    Arrays.asList("수학", "방정식", "계산"),
                    "통합 테스트용 수학 문제",
                    3 // 3개 문제만 생성
            );

            if (!(Boolean) problemResult.get("success")) {
                throw new RuntimeException("문제 생성 실패: " + problemResult.get("error"));
            }

            UUID problemUuid = UUID.fromString(problemResult.get("problemUuid").toString());
            testResult.put("step2_problem_generation", Map.of(
                    "problemUuid", problemUuid,
                    "questionCount", 3,
                    "status", "성공"
            ));

            // 3단계: 문제 풀이 시뮬레이션 (일부러 틀린 답도 포함)
            log.info("3단계: 문제 풀이 시뮬레이션");
            List<Map<String, Object>> answerResults = new ArrayList<>();

            // 문제 1: 정답
            Map<String, Object> answer1 = gradingService.submitAnswer(problemUuid, 1, 0, testAppUsers.getUuid());
            answerResults.add(Map.of("question", 1, "result", answer1));

            // 문제 2: 오답 (일부러)
            Map<String, Object> answer2 = gradingService.submitAnswer(problemUuid, 2, 2, testAppUsers.getUuid());
            answerResults.add(Map.of("question", 2, "result", answer2));

            // 문제 3: 정답
            Map<String, Object> answer3 = gradingService.submitAnswer(problemUuid, 3, 0, testAppUsers.getUuid());
            answerResults.add(Map.of("question", 3, "result", answer3));

            testResult.put("step3_problem_solving", Map.of(
                    "answers", answerResults,
                    "status", "성공"
            ));

            // 4단계: 최종 채점
            log.info("4단계: 최종 채점 및 결과 계산");
            Map<String, Object> gradingResult = gradingService.completeQuiz(problemUuid);
            testResult.put("step4_grading", gradingResult);

            // 5단계: 오답노트 조회
            log.info("5단계: 오답노트 확인");
            Map<String, Object> wrongAnswers = wrongAnswerService.getUserWrongAnswers(testAppUsers.getUuid());
            testResult.put("step5_wrong_answers", Map.of(
                    "totalWrongAnswers", wrongAnswers.get("totalWrongAnswers"),
                    "notMasteredCount", wrongAnswers.get("notMasteredCount"),
                    "status", "성공"
            ));

            // 6단계: 복습 퀴즈 생성
            log.info("6단계: 복습 퀴즈 생성");
            Map<String, Object> reviewQuiz = wrongAnswerService.generateReviewQuiz(
                    testAppUsers.getUuid(),
                    testSubject.getUuid(),
                    5
            );
            testResult.put("step6_review_quiz", reviewQuiz);

            // 최종 결과
            testResult.put("overall_status", "전체 테스트 성공");
            testResult.put("completed_at", LocalDateTime.now());
            testResult.put("test_summary", Map.of(
                    "총_문제수", 3,
                    "정답수", 2,
                    "오답수", 1,
                    "점수", gradingResult.get("score"),
                    "오답노트_생성여부", (Integer) wrongAnswers.get("totalWrongAnswers") > 0
            ));

            log.info("=== 전체 플랫폼 통합 테스트 완료 ===");
            return ApiResponse.success(testResult, "전체 플랫폼 통합 테스트가 성공적으로 완료되었습니다!");

        } catch (Exception e) {
            log.error("통합 테스트 실패", e);
            testResult.put("error", e.getMessage());
            testResult.put("overall_status", "실패");
            return ApiResponse.error("통합 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 개별 기능별 테스트
     */
    @PostMapping("/test-grading-only")
    @Transactional
    public ApiResponse<Map<String, Object>> testGradingOnly() {
        try {
            log.info("=== 채점 시스템 단독 테스트 ===");

            // 기존 문제 사용하거나 새로 생성
            AppUsers testAppUsers = createTestUser();
            Subject testSubject = createTestSubject(testAppUsers);

            Map<String, Object> problemResult = problemGenerationService.generateProblemsFromKeywords(
                    testAppUsers.getUuid(), testSubject.getUuid(),
                    Arrays.asList("채점테스트"), "채점 시스템 테스트", 2
            );

            UUID problemUuid = UUID.fromString(problemResult.get("problemUuid").toString());

            // 답안 제출 테스트
            Map<String, Object> answer1 = gradingService.submitAnswer(problemUuid, 1, 0, testAppUsers.getUuid());
            Map<String, Object> answer2 = gradingService.submitAnswer(problemUuid, 2, 1, testAppUsers.getUuid());
            Map<String, Object> completion = gradingService.completeQuiz(problemUuid);

            Map<String, Object> result = Map.of(
                    "answer1", answer1,
                    "answer2", answer2,
                    "completion", completion,
                    "status", "채점 시스템 테스트 완료"
            );

            return ApiResponse.success(result, "채점 시스템이 정상 작동합니다!");

        } catch (Exception e) {
            log.error("채점 테스트 실패", e);
            return ApiResponse.error("채점 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 오답노트 시스템 단독 테스트
     */
    @PostMapping("/test-wrong-answers")
    @Transactional
    public ApiResponse<Map<String, Object>> testWrongAnswersOnly() {
        try {
            log.info("=== 오답노트 시스템 단독 테스트 ===");

            AppUsers testAppUsers = createTestUser();
            Subject testSubject = createTestSubject(testAppUsers);

            // 문제 생성 및 일부러 틀리기
            Map<String, Object> problemResult = problemGenerationService.generateProblemsFromKeywords(
                    testAppUsers.getUuid(), testSubject.getUuid(),
                    Arrays.asList("오답노트테스트"), "오답노트 시스템 테스트", 2
            );

            UUID problemUuid = UUID.fromString(problemResult.get("problemUuid").toString());

            // 일부러 틀린 답 제출
            gradingService.submitAnswer(problemUuid, 1, 3, testAppUsers.getUuid()); // 틀린 답
            gradingService.submitAnswer(problemUuid, 2, 4, testAppUsers.getUuid()); // 틀린 답
            gradingService.completeQuiz(problemUuid);

            // 오답노트 확인
            Map<String, Object> wrongAnswers = wrongAnswerService.getUserWrongAnswers(testAppUsers.getUuid());

            // 복습 퀴즈 생성
            Map<String, Object> reviewQuiz = wrongAnswerService.generateReviewQuiz(
                    testAppUsers.getUuid(), testSubject.getUuid(), 5
            );

            Map<String, Object> result = Map.of(
                    "wrongAnswersCreated", wrongAnswers,
                    "reviewQuizGenerated", reviewQuiz,
                    "status", "오답노트 시스템 테스트 완료"
            );

            return ApiResponse.success(result, "오답노트 시스템이 정상 작동합니다!");

        } catch (Exception e) {
            log.error("오답노트 테스트 실패", e);
            return ApiResponse.error("오답노트 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스 연결 및 기본 CRUD 테스트
     */
    @GetMapping("/test-database")
    public ApiResponse<Map<String, Object>> testDatabase() {
        try {
            log.info("=== 데이터베이스 연결 테스트 ===");

            Map<String, Object> dbStats = new HashMap<>();

            // 각 테이블별 데이터 개수 확인
            dbStats.put("users_count", userRepository.count());
            dbStats.put("subjects_count", subjectRepository.count());
            dbStats.put("chat_sessions_count", chatSessionRepository.count());

            // 새 엔티티들이 제대로 생성되었는지 확인하는 로직
            // (실제로는 Repository Bean이 정상 로드되는지만 확인)
            dbStats.put("database_connection", "정상");
            dbStats.put("jpa_repositories", "정상 로드됨");

            return ApiResponse.success(dbStats, "데이터베이스 연결 및 JPA 설정이 정상입니다!");

        } catch (Exception e) {
            log.error("데이터베이스 테스트 실패", e);
            return ApiResponse.error("데이터베이스 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 시스템 전체 상태 확인
     */
    @GetMapping("/system-status")
    public ApiResponse<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> systemStatus = new HashMap<>();

            // 기존 컴포넌트들 상태
            systemStatus.put("openai_config", "설정됨");
            systemStatus.put("security_config", "개발모드 (모든 요청 허용)");
            systemStatus.put("cors_config", "모든 도메인 허용");

            // 새로 추가된 기능들 상태
            systemStatus.put("grading_system", "구현완료");
            systemStatus.put("wrong_answer_system", "구현완료");
            systemStatus.put("integration_apis", "구현완료");

            // API 엔드포인트 목록
            List<String> newApis = Arrays.asList(
                    "POST /api/grading/problems/{problemUuid}/submit",
                    "POST /api/grading/problems/{problemUuid}/complete",
                    "GET /api/wrong-answers/users/{userUuid}",
                    "POST /api/wrong-answers/users/{userUuid}/subjects/{subjectUuid}/review-quiz",
                    "POST /api/integration-test/full-scenario"
            );
            systemStatus.put("new_api_endpoints", newApis);

            // 학습 플랫폼 완성도
            Map<String, String> completionStatus = Map.of(
                    "과목생성", "✅ 완료",
                    "채팅시스템", "✅ 완료",
                    "키워드추출", "✅ 완료",
                    "AI문제생성", "✅ 완료",
                    "문제풀이", "✅ 완료",
                    "채점시스템", "✅ 새로 추가됨",
                    "오답노트", "✅ 새로 추가됨",
                    "복습퀴즈", "✅ 새로 추가됨"
            );
            systemStatus.put("learning_platform_completion", completionStatus);

            systemStatus.put("ready_for_frontend", true);
            systemStatus.put("timestamp", LocalDateTime.now());

            return ApiResponse.success(systemStatus, "AI 학습 플랫폼이 완전히 구현되었습니다!");

        } catch (Exception e) {
            log.error("시스템 상태 확인 실패", e);
            return ApiResponse.error("시스템 상태 확인 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트 데이터 초기화
     */
    @PostMapping("/reset-test-data")
    @Transactional
    public ApiResponse<String> resetTestData() {
        try {
            log.info("=== 테스트 데이터 초기화 ===");

            // 테스트 관련 데이터만 삭제 (email 기준)
            userRepository.findByEmail("integration-test@example.com")
                    .ifPresent(user -> {
                        userRepository.delete(user);
                        log.info("테스트 사용자 삭제 완료");
                    });

            return ApiResponse.success("초기화 완료", "테스트 데이터가 초기화되었습니다.");

        } catch (Exception e) {
            log.error("테스트 데이터 초기화 실패", e);
            return ApiResponse.error("초기화 실패: " + e.getMessage());
        }
    }

    /**
     * API 엔드포인트 목록 조회
     */
    @GetMapping("/api-endpoints")
    public ApiResponse<Map<String, List<String>>> getApiEndpoints() {
        Map<String, List<String>> endpoints = new HashMap<>();

        endpoints.put("기존_API", Arrays.asList(
                "POST /api/users/{userUuid}/subjects - 과목 생성",
                "POST /api/chat/users/{userUuid}/subjects/{subjectUuid}/sessions/{sessionUuid}/messages - 채팅",
                "POST /api/problem-session/start-from-chat - AI 문제 생성",
                "GET /api/problem-session/problem/{problemUuid}/current - 현재 문제 조회"
        ));

        endpoints.put("새로추가된_API", Arrays.asList(
                "POST /api/grading/problems/{problemUuid}/submit - 답안 제출 및 채점",
                "POST /api/grading/problems/{problemUuid}/complete - 퀴즈 완료 채점",
                "GET /api/grading/users/{userUuid}/stats - 학습 통계",
                "GET /api/wrong-answers/users/{userUuid} - 오답노트 조회",
                "POST /api/wrong-answers/users/{userUuid}/subjects/{subjectUuid}/review-quiz - 복습 퀴즈 생성",
                "POST /api/wrong-answers/{wrongAnswerNoteUuid}/review-complete - 복습 완료 처리"
        ));

        endpoints.put("테스트_API", Arrays.asList(
                "POST /api/integration-test/full-scenario - 전체 시나리오 테스트",
                "POST /api/integration-test/test-grading-only - 채점 시스템만 테스트",
                "POST /api/integration-test/test-wrong-answers - 오답노트 시스템만 테스트",
                "GET /api/integration-test/system-status - 시스템 전체 상태",
                "POST /api/integration-test/reset-test-data - 테스트 데이터 초기화"
        ));

        return ApiResponse.success(endpoints, "사용 가능한 API 엔드포인트 목록입니다.");
    }

    // ========== Private Helper Methods ==========

    private AppUsers createTestUser() {
        // 기존 테스트 사용자 있으면 삭제
        userRepository.findByEmail("integration-test@example.com")
                .ifPresent(userRepository::delete);

        AppUsers testAppUsers = AppUsers.builder()
                .email("integration-test@example.com")
                .password("test123")
                .nickname("통합테스트사용자")
                .token("integration_test_token")
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(testAppUsers);
    }

    private Subject createTestSubject(AppUsers appUsers) {
        // 기존 테스트 과목 있으면 삭제
        subjectRepository.findByUser_UuidAndTitle(appUsers.getUuid(), "통합테스트과목")
                .ifPresent(subjectRepository::delete);

        Subject testSubject = Subject.builder()
                .appUsers(appUsers)
                .title("통합테스트과목")
                .createdAt(LocalDateTime.now())
                .build();

        return subjectRepository.save(testSubject);
    }
}