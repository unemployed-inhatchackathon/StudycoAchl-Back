package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.domain.Exams;
import com.studycoAchl.hackaton.domain.Problem;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.studycoAchl.hackaton.repository.ExamsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class ProblemSessionService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ExamsRepository examsRepository;

    @Autowired
    private ChatRoomIntegrationService chatRoomService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> createProblemSession(String userUuid, String subjectUuid,
                                                    String sessionTitle, Integer questionCount,
                                                    String sourceType, String sourceContent) {
        try {
            // 1. 시험 정보 생성
            String examUuid = UUID.randomUUID().toString();
            Exams exam = new Exams(examUuid, sessionTitle, questionCount, userUuid, subjectUuid);
            Exams savedExam = examsRepository.save(exam);

            // 2. 문제 생성 (ChatRoomIntegrationService 사용)
            String chatSessionUuid = UUID.randomUUID().toString();
            Map<String, Object> problemResult = chatRoomService.generateProblemsFromChatContent(
                    userUuid, subjectUuid, chatSessionUuid, sourceContent, questionCount
            );

            if (!(Boolean) problemResult.get("success")) {
                return Map.of("success", false, "error", "문제 생성 실패");
            }

            // 3. 성공 결과 반환
            return Map.of(
                    "success", true,
                    "examUuid", savedExam.getUuid(),
                    "problemUuid", problemResult.get("problemUuid"),
                    "sessionTitle", sessionTitle,
                    "totalQuestions", questionCount,
                    "problems", problemResult.get("problems"),
                    "message", "문제풀이 세션이 시작되었습니다!",
                    "currentQuestionIndex", 0
            );

        } catch (Exception e) {
            e.printStackTrace(); // 디버깅용
            return Map.of("success", false, "error", "세션 생성 실패: " + e.getMessage());
        }
    }

    // 누락된 메서드 1: getCurrentQuestion
    public Map<String, Object> getCurrentQuestion(String problemUuid, Integer questionIndex) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) {
                return Map.of("success", false, "error", "문제를 찾을 수 없습니다");
            }

            ObjectNode root = (ObjectNode) objectMapper.readTree(problem.getProblems());
            ArrayNode problemsArray = (ArrayNode) root.get("problems");

            if (questionIndex >= problemsArray.size()) {
                return Map.of("success", false, "error", "문제 인덱스가 범위를 벗어났습니다");
            }

            ObjectNode currentQuestion = (ObjectNode) problemsArray.get(questionIndex);

            ArrayNode optionsArray = (ArrayNode) currentQuestion.get("options");
            List<String> options = new ArrayList<>();
            for (int i = 0; i < optionsArray.size(); i++) {
                options.add(optionsArray.get(i).asText());
            }

            return Map.of(
                    "success", true,
                    "questionId", currentQuestion.get("id").asInt(),
                    "questionText", currentQuestion.get("question").asText(),
                    "options", options,
                    "currentQuestionIndex", questionIndex,
                    "totalQuestions", problemsArray.size(),
                    "progress", Math.round((double) (questionIndex + 1) / problemsArray.size() * 100)
            );

        } catch (Exception e) {
            return Map.of("success", false, "error", "문제 조회 실패: " + e.getMessage());
        }
    }

    // 누락된 메서드 2: submitAnswerAndGetNext
    public Map<String, Object> submitAnswerAndGetNext(String problemUuid, Integer questionId,
                                                      Integer selectedAnswer, String userUuid) {
        try {
            // 답안 체크 (ChatRoomIntegrationService 재사용)
            Map<String, Object> answerResult = chatRoomService.processAnswerSubmission(
                    problemUuid, questionId, selectedAnswer, userUuid
            );

            if (!(Boolean) answerResult.get("success")) {
                return answerResult;
            }

            // 다음 문제 확인
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            ObjectNode root = (ObjectNode) objectMapper.readTree(problem.getProblems());
            ArrayNode problemsArray = (ArrayNode) root.get("problems");

            Integer nextQuestionIndex = questionId; // questionId는 1부터 시작하므로 다음 인덱스
            boolean hasNextQuestion = nextQuestionIndex < problemsArray.size();

            Map<String, Object> result = new HashMap<>(answerResult);
            result.put("hasNextQuestion", hasNextQuestion);
            result.put("nextQuestionIndex", hasNextQuestion ? nextQuestionIndex : null);
            result.put("totalQuestions", problemsArray.size());

            if (hasNextQuestion) {
                // 다음 문제 정보 추가
                Map<String, Object> nextQuestion = getCurrentQuestion(problemUuid, nextQuestionIndex);
                if ((Boolean) nextQuestion.get("success")) {
                    result.put("nextQuestion", nextQuestion);
                }
            } else {
                result.put("message", "모든 문제를 완료했습니다! 🎉");
                result.put("sessionCompleted", true);
            }

            return result;

        } catch (Exception e) {
            return Map.of("success", false, "error", "답안 처리 실패: " + e.getMessage());
        }
    }

    // 사용자별 시험 목록 조회
    public Map<String, Object> getUserExams(String userUuid) {
        try {
            List<Exams> exams = examsRepository.findByUserUuidOrderByCreatedAtDesc(userUuid);

            List<Map<String, Object>> examList = new ArrayList<>();
            for (Exams exam : exams) {
                Map<String, Object> examData = new HashMap<>();
                examData.put("examUuid", exam.getUuid());
                examData.put("title", exam.getTitle());
                examData.put("questionCount", exam.getProSu());
                examData.put("createdAt", exam.getCreatedAt());
                examList.add(examData);
            }

            return Map.of(
                    "success", true,
                    "exams", examList,
                    "totalExams", exams.size()
            );

        } catch (Exception e) {
            return Map.of("success", false, "error", "시험 목록 조회 실패: " + e.getMessage());
        }
    }
}