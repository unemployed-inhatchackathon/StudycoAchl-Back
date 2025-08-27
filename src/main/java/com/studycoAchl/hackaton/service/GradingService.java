package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.*;
import com.studycoAchl.hackaton.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradingService {

    private final QuizResultRepository quizResultRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final WrongAnswerNoteRepository wrongAnswerNoteRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    // Bean 주입 대신 직접 생성하여 순환 의존성 방지
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 단일 문제 답안 제출 및 즉시 채점
     */
    @Transactional
    public Map<String, Object> submitAnswer(UUID problemUuid, int questionNumber,
                                            int selectedAnswer, UUID userUuid) {
        try {
            log.info("답안 제출 - problemUuid: {}, questionNumber: {}, selectedAnswer: {}",
                    problemUuid, questionNumber, selectedAnswer);

            // 1. Problem과 User 조회
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다: " + problemUuid));

            AppUsers appUsers = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userUuid));

            // 2. Problem JSON에서 정답 확인 - 안전한 파싱
            String problemsJsonStr = problem.getProblems();
            if (problemsJsonStr == null || problemsJsonStr.isEmpty()) {
                throw new RuntimeException("문제 데이터가 없습니다.");
            }

            JsonNode problemsNode = objectMapper.readTree(problemsJsonStr);
            JsonNode questionsArray = problemsNode.get("questions");

            if (questionsArray == null || !questionsArray.isArray()) {
                throw new RuntimeException("문제 배열을 찾을 수 없습니다.");
            }

            if (questionNumber < 1 || questionNumber > questionsArray.size()) {
                throw new RuntimeException("유효하지 않은 문제 번호입니다: " + questionNumber);
            }

            JsonNode currentQuestion = questionsArray.get(questionNumber - 1);

            // 안전한 필드 추출
            int correctAnswer = currentQuestion.path("correctAnswer").asInt(0);
            String questionText = currentQuestion.path("question").asText("문제 텍스트가 없습니다.");
            String keyword = currentQuestion.path("keyword").asText("일반");
            String explanation = currentQuestion.path("explanation").asText("해설이 없습니다.");

            // 3. 정답 여부 판정
            boolean isCorrect = (selectedAnswer == correctAnswer);

            // 4. QuizResult 조회 또는 생성
            QuizResult quizResult = quizResultRepository.findByProblem_Uuid(problemUuid)
                    .orElseGet(() -> createNewQuizResult(problem, appUsers));

            // 5. UserAnswer 생성 및 저장
            UserAnswer userAnswer = UserAnswer.builder()
                    .quizResult(quizResult)
                    .questionId(problemUuid + "_" + questionNumber)
                    .questionNumber(questionNumber)
                    .questionText(questionText)
                    .selectedAnswer(selectedAnswer)
                    .correctAnswer(correctAnswer)
                    .isCorrect(isCorrect)
                    .keyword(keyword)
                    .answeredAt(LocalDateTime.now())
                    .build();

            UserAnswer savedAnswer = userAnswerRepository.save(userAnswer);

            // 6. 틀린 문제면 오답노트에 추가
            if (!isCorrect) {
                createWrongAnswerNote(savedAnswer, currentQuestion);
            }

            // 7. 응답 생성
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("isCorrect", isCorrect);
            result.put("correctAnswer", correctAnswer);
            result.put("selectedAnswer", selectedAnswer);
            result.put("explanation", explanation);
            result.put("keyword", keyword);
            result.put("userAnswerUuid", savedAnswer.getUuid());
            result.put("questionNumber", questionNumber);
            result.put("message", isCorrect ? "정답입니다!" : "틀렸습니다. 오답노트에 추가되었습니다.");

            log.info("답안 제출 완료 - isCorrect: {}", isCorrect);
            return result;

        } catch (Exception e) {
            log.error("답안 제출 처리 실패 - problemUuid: {}", problemUuid, e);
            return Map.of(
                    "success", false,
                    "error", "답안 처리 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 퀴즈 완료 후 최종 채점
     */
    @Transactional
    public Map<String, Object> completeQuiz(UUID problemUuid) {
        try {
            log.info("퀴즈 완료 채점 - problemUuid: {}", problemUuid);

            // QuizResult 조회
            QuizResult quizResult = quizResultRepository.findByProblem_Uuid(problemUuid)
                    .orElseThrow(() -> new RuntimeException("퀴즈 결과를 찾을 수 없습니다."));

            // 모든 답안 조회
            List<UserAnswer> allAnswers = userAnswerRepository.findByQuizResult_Uuid(quizResult.getUuid());

            if (allAnswers.isEmpty()) {
                throw new RuntimeException("제출된 답안이 없습니다.");
            }

            // 통계 계산
            long correctCount = allAnswers.stream().mapToLong(ua -> Boolean.TRUE.equals(ua.getIsCorrect()) ? 1 : 0).sum();
            int totalQuestions = allAnswers.size();

            // QuizResult 업데이트
            quizResult.setTotalQuestions(totalQuestions);
            quizResult.setCorrectAnswers((int) correctCount);
            quizResult.setWrongAnswers(totalQuestions - (int) correctCount);
            quizResult.setCompletedAt(LocalDateTime.now());
            quizResult.setStatus(QuizResult.ResultStatus.COMPLETED);

            // 시간 계산 추가
            if (quizResult.getStartedAt() != null) {
                long minutes = java.time.temporal.ChronoUnit.MINUTES.between(quizResult.getStartedAt(), LocalDateTime.now());
                quizResult.setTimeTakenMinutes((int) minutes);
            }

            quizResult.calculateScore();

            QuizResult finalResult = quizResultRepository.save(quizResult);

            // 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("quizResultUuid", finalResult.getUuid());
            result.put("totalQuestions", totalQuestions);
            result.put("correctAnswers", correctCount);
            result.put("wrongAnswers", totalQuestions - correctCount);
            result.put("score", finalResult.getScore());
            result.put("accuracyRate", finalResult.getAccuracyRate());
            result.put("timeTakenMinutes", finalResult.getTimeTakenMinutes());
            result.put("completedAt", finalResult.getCompletedAt());
            result.put("hasWrongAnswers", correctCount < totalQuestions);

            // 격려 메시지 추가
            String encouragementMessage = generateEncouragementMessage((int) correctCount, totalQuestions);
            result.put("encouragementMessage", encouragementMessage);

            log.info("퀴즈 완료 채점 성공 - score: {}점", finalResult.getScore());
            return result;

        } catch (Exception e) {
            log.error("퀴즈 완료 채점 실패 - problemUuid: {}", problemUuid, e);
            return Map.of(
                    "success", false,
                    "error", "퀴즈 채점 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 퀴즈 결과 조회
     */
    @Transactional(readOnly = true)
    public QuizResult getQuizResult(UUID problemUuid) {
        return quizResultRepository.findByProblem_Uuid(problemUuid)
                .orElseThrow(() -> new RuntimeException("퀴즈 결과를 찾을 수 없습니다."));
    }

    /**
     * 사용자 학습 통계
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(UUID userUuid) {
        try {
            Double averageScore = quizResultRepository.getAverageScoreByUser(userUuid);
            Long totalQuizzes = quizResultRepository.countCompletedQuizzesByUser(userUuid);
            List<UserAnswer> wrongAnswers = userAnswerRepository.findWrongAnswersByUser(userUuid);

            // 최근 퀴즈 결과들
            List<QuizResult> recentQuizzes = quizResultRepository.findByAppUsers_UuidOrderByCompletedAtDesc(userUuid);

            // 오답노트 통계
            List<WrongAnswerNote> totalWrongNotes = wrongAnswerNoteRepository.findByAppUsers_Uuid(userUuid);
            List<WrongAnswerNote> masteredNotes = wrongAnswerNoteRepository.findByAppUsers_UuidAndIsMastered(userUuid, true);

            Map<String, Object> stats = new HashMap<>();
            stats.put("averageScore", averageScore != null ? Math.round(averageScore * 100.0) / 100.0 : 0.0);
            stats.put("totalQuizzes", totalQuizzes);
            stats.put("totalWrongAnswers", wrongAnswers.size());
            stats.put("totalWrongNotes", totalWrongNotes.size());
            stats.put("masteredWrongNotes", masteredNotes.size());
            stats.put("needsReviewCount", totalWrongNotes.size() - masteredNotes.size());

            // 최근 성과 (최근 5개 퀴즈) - 타입 명시적 지정으로 해결
            List<Map<String, Object>> recentPerformance = new ArrayList<>();
            recentQuizzes.stream()
                    .limit(5)
                    .forEach(quiz -> {
                        Map<String, Object> performance = new HashMap<>();
                        performance.put("score", quiz.getScore());
                        performance.put("completedAt", quiz.getCompletedAt());
                        performance.put("subjectTitle", quiz.getSubject() != null ? quiz.getSubject().getTitle() : "일반");
                        recentPerformance.add(performance);
                    });

            stats.put("recentPerformance", recentPerformance);

            return stats;

        } catch (Exception e) {
            log.error("사용자 통계 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("사용자 통계 조회 실패: " + e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 새 QuizResult 생성
     */
    @Transactional
    protected QuizResult createNewQuizResult(Problem problem, AppUsers appUsers) {
        try {
            QuizResult quizResult = QuizResult.builder()
                    .problem(problem)
                    .appUsers(appUsers)
                    .subject(problem.getSubject())
                    .status(QuizResult.ResultStatus.IN_PROGRESS)
                    .startedAt(LocalDateTime.now())
                    .correctAnswers(0)
                    .wrongAnswers(0)
                    .userAnswers(new ArrayList<>())
                    .build();

            return quizResultRepository.save(quizResult);
        } catch (Exception e) {
            log.error("QuizResult 생성 실패", e);
            throw new RuntimeException("퀴즈 결과 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 오답노트 생성 - 안전한 JSON 처리
     */
    @Transactional
    protected void createWrongAnswerNote(UserAnswer userAnswer, JsonNode questionNode) {
        try {
            // 선택지 JSON 생성 - null 안전성 추가
            JsonNode optionsNode = questionNode.get("options");
            String optionsJson;

            if (optionsNode != null && optionsNode.isArray()) {
                optionsJson = objectMapper.writeValueAsString(optionsNode);
            } else {
                // 기본 선택지 생성
                optionsJson = "[\"선택지 1\",\"선택지 2\",\"선택지 3\",\"선택지 4\",\"선택지 5\"]";
            }

            WrongAnswerNote wrongNote = WrongAnswerNote.builder()
                    .appUsers(userAnswer.getQuizResult().getAppUsers())
                    .subject(userAnswer.getQuizResult().getSubject())
                    .userAnswer(userAnswer)
                    .questionText(userAnswer.getQuestionText())
                    .options(optionsJson)
                    .correctAnswer(userAnswer.getCorrectAnswer())
                    .userWrongAnswer(userAnswer.getSelectedAnswer())
                    .explanation(questionNode.path("explanation").asText("해설이 제공되지 않았습니다."))
                    .keyword(userAnswer.getKeyword() != null ? userAnswer.getKeyword() : "일반")
                    .reviewCount(0)
                    .isMastered(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            WrongAnswerNote savedNote = wrongAnswerNoteRepository.save(wrongNote);
            log.info("오답노트 생성 완료 - uuid: {}, keyword: {}", savedNote.getUuid(), savedNote.getKeyword());

        } catch (Exception e) {
            log.error("오답노트 생성 실패 - userAnswerUuid: {}", userAnswer.getUuid(), e);
            // 오답노트 생성 실패해도 답안 제출은 계속 진행되도록 함
        }
    }

    /**
     * 격려 메시지 생성
     */
    private String generateEncouragementMessage(int correctCount, int totalQuestions) {
        double accuracy = (double) correctCount / totalQuestions * 100;

        if (accuracy >= 90) {
            return "훌륭합니다! 거의 완벽한 점수네요!";
        } else if (accuracy >= 70) {
            return "잘했습니다! 좋은 성과입니다!";
        } else if (accuracy >= 50) {
            return "괜찮습니다. 조금 더 공부하면 더 좋은 결과를 얻을 수 있을 거예요!";
        } else {
            return "다시 한번 복습해보세요. 오답노트를 활용하면 도움이 될 거예요!";
        }
    }

    /**
     * 문제별 상세 분석
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuestionAnalysis(UUID userAnswerUuid) {
        try {
            UserAnswer userAnswer = userAnswerRepository.findById(userAnswerUuid)
                    .orElseThrow(() -> new RuntimeException("답안을 찾을 수 없습니다."));

            Map<String, Object> analysis = new HashMap<>();
            analysis.put("questionNumber", userAnswer.getQuestionNumber());
            analysis.put("isCorrect", userAnswer.getIsCorrect());
            analysis.put("selectedAnswer", userAnswer.getSelectedAnswer());
            analysis.put("correctAnswer", userAnswer.getCorrectAnswer());
            analysis.put("keyword", userAnswer.getKeyword());
            analysis.put("answeredAt", userAnswer.getAnsweredAt());

            // 시간 정보 추가
            if (userAnswer.getTimeSpentSeconds() != null) {
                analysis.put("timeSpentSeconds", userAnswer.getTimeSpentSeconds());
                analysis.put("timeCategory", categorizeTime(userAnswer.getTimeSpentSeconds()));
            }

            return analysis;

        } catch (Exception e) {
            log.error("문제 분석 실패 - userAnswerUuid: {}", userAnswerUuid, e);
            throw new RuntimeException("문제 분석 실패: " + e.getMessage());
        }
    }

    /**
     * 시간 카테고리 분류
     */
    private String categorizeTime(int timeSpentSeconds) {
        if (timeSpentSeconds < 30) {
            return "매우 빠름";
        } else if (timeSpentSeconds < 60) {
            return "빠름";
        } else if (timeSpentSeconds < 120) {
            return "적당함";
        } else {
            return "신중함";
        }
    }

    /**
     * 키워드별 성과 분석
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKeywordPerformance(UUID userUuid) {
        try {
            List<UserAnswer> allAnswers = userAnswerRepository.findWrongAnswersByUser(userUuid);

            Map<String, List<UserAnswer>> keywordGroups = allAnswers.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            ua -> ua.getKeyword() != null ? ua.getKeyword() : "일반"
                    ));

            Map<String, Map<String, Object>> keywordStats = new HashMap<>();

            for (Map.Entry<String, List<UserAnswer>> entry : keywordGroups.entrySet()) {
                String keyword = entry.getKey();
                List<UserAnswer> answers = entry.getValue();

                long correctCount = answers.stream().mapToLong(ua -> Boolean.TRUE.equals(ua.getIsCorrect()) ? 1 : 0).sum();
                double accuracy = answers.isEmpty() ? 0.0 : (double) correctCount / answers.size() * 100.0;

                Map<String, Object> keywordStat = new HashMap<>();
                keywordStat.put("totalAnswers", answers.size());
                keywordStat.put("correctAnswers", correctCount);
                keywordStat.put("wrongAnswers", answers.size() - correctCount);
                keywordStat.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
                keywordStat.put("needsReview", accuracy < 70.0);

                keywordStats.put(keyword, keywordStat);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("keywordPerformance", keywordStats);
            result.put("totalKeywords", keywordStats.size());

            // 가장 취약한 키워드 찾기
            String weakestKeyword = keywordStats.entrySet().stream()
                    .min(Map.Entry.<String, Map<String, Object>>comparingByValue(
                            (a, b) -> Double.compare((Double) a.get("accuracy"), (Double) b.get("accuracy"))
                    ))
                    .map(Map.Entry::getKey)
                    .orElse("없음");

            result.put("weakestKeyword", weakestKeyword);

            return result;

        } catch (Exception e) {
            log.error("키워드별 성과 분석 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("성과 분석 실패: " + e.getMessage());
        }
    }
}