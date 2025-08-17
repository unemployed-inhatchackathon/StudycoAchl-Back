package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.domain.Problem;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatRoomIntegrationService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired(required = false)
    private QuestionGenerationService questionGenerationService;

    @Value("${app.use-real-ai:false}")
    private boolean useRealAI;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 채팅 내용 기반 문제 생성
    public Map<String, Object> generateProblemsFromChatContent(
            String userUuid, String subjectUuid, String chatSessionUuid,
            String chatContent, Integer questionCount) {

        try {
            String problemJson;

            if (useRealAI && questionGenerationService != null) {
                // 실제 OpenAI 사용 (크레딧 사용)
                System.out.println("🤖 실제 OpenAI로 문제 생성 중...");
                problemJson = questionGenerationService.generateQuestions(
                        "학습", chatContent, questionCount);

                // OpenAI 응답을 우리 형식으로 파싱
                problemJson = convertAIResponseToFormat(problemJson, questionCount);
            } else {
                // 목업 데이터 사용 (크레딧 절약)
                System.out.println("🎯 목업 데이터로 문제 생성 중...");
                problemJson = generateMockProblemsFromChat(chatContent, questionCount);
            }

            // 데이터베이스에 저장
            Problem problem = new Problem(
                    UUID.randomUUID().toString(),
                    problemJson,
                    userUuid,
                    subjectUuid,
                    chatSessionUuid
            );

            Problem savedProblem = problemRepository.save(problem);

            // 채팅방에서 사용할 형태로 반환
            return Map.of(
                    "success", true,
                    "problemUuid", savedProblem.getUuid(),
                    "problems", parseProblemsForChat(problemJson),
                    "totalQuestions", questionCount,
                    "message", questionCount + "개의 문제가 생성되었습니다!",
                    "createdAt", savedProblem.getCreatedAt()
            );

        } catch (Exception e) {
            e.printStackTrace(); // 디버깅용
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }

    // 채팅 내용 기반 목업 문제 생성
    private String generateMockProblemsFromChat(String chatContent, Integer questionCount) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("title", "채팅 기반 생성 문제");
            root.put("totalQuestions", questionCount);
            root.put("source", "chat");
            root.put("chatContent", chatContent);

            ArrayNode problemsArray = objectMapper.createArrayNode();

            String keyword = extractKeyword(chatContent);

            for (int i = 1; i <= questionCount; i++) {
                ObjectNode problem = objectMapper.createObjectNode();
                problem.put("id", i);

                // 채팅 내용에 따른 문제 생성
                if (keyword.equals("수학")) {
                    problem.put("question", generateMathQuestion(i));
                    ArrayNode options = objectMapper.createArrayNode();
                    options.add("10");
                    options.add("15"); // 정답
                    options.add("20");
                    options.add("25");
                    problem.set("options", options);
                    problem.put("correctAnswer", 1); // 2번이 정답 (0-based index)
                    problem.put("explanation", "수학 계산: 5 × 3 = 15입니다.");
                } else if (keyword.equals("영어")) {
                    problem.put("question", generateEnglishQuestion(i));
                    ArrayNode options = objectMapper.createArrayNode();
                    options.add("go");
                    options.add("went"); // 정답
                    options.add("gone");
                    options.add("going");
                    problem.set("options", options);
                    problem.put("correctAnswer", 1);
                    problem.put("explanation", "과거형: go의 과거형은 went입니다.");
                } else {
                    problem.put("question", String.format("문제 %d: %s 관련 질문입니다.", i, keyword));
                    ArrayNode options = objectMapper.createArrayNode();
                    options.add("선택지 1");
                    options.add("선택지 2 (정답)");
                    options.add("선택지 3");
                    options.add("선택지 4");
                    problem.set("options", options);
                    problem.put("correctAnswer", 1);
                    problem.put("explanation", "채팅 내용을 바탕으로 한 해설입니다.");
                }

                // 선택지별 설명 추가
                ArrayNode explanations = objectMapper.createArrayNode();
                explanations.add("선택지 1에 대한 설명");
                explanations.add("선택지 2가 정답인 이유");
                explanations.add("선택지 3이 틀린 이유");
                explanations.add("선택지 4가 틀린 이유");
                problem.set("optionExplanations", explanations);

                problemsArray.add(problem);
            }

            root.set("problems", problemsArray);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException("목업 문제 생성 실패", e);
        }
    }

    // 수학 문제 생성
    private String generateMathQuestion(int questionNum) {
        String[] mathQuestions = {
                "5 × 3 = ?",
                "12 ÷ 4 = ?",
                "8 + 7 = ?",
                "20 - 6 = ?",
                "9 × 2 = ?"
        };
        return mathQuestions[(questionNum - 1) % mathQuestions.length];
    }

    // 영어 문제 생성
    private String generateEnglishQuestion(int questionNum) {
        String[] englishQuestions = {
                "I _____ to school yesterday. (과거형을 고르세요)",
                "She _____ a book now. (현재진행형을 고르세요)",
                "They _____ homework every day. (현재형을 고르세요)"
        };
        return englishQuestions[(questionNum - 1) % englishQuestions.length];
    }

    // 채팅 내용에서 키워드 추출
    private String extractKeyword(String chatContent) {
        if (chatContent == null) return "일반";

        String content = chatContent.toLowerCase();
        if (content.contains("수학") || content.contains("math") || content.contains("계산") ||
                content.contains("덧셈") || content.contains("뺄셈") || content.contains("곱셈") || content.contains("나눗셈")) {
            return "수학";
        }
        if (content.contains("영어") || content.contains("english") || content.contains("단어") || content.contains("문법")) {
            return "영어";
        }
        if (content.contains("과학") || content.contains("science") || content.contains("물리") || content.contains("화학")) {
            return "과학";
        }
        if (content.contains("역사") || content.contains("history")) {
            return "역사";
        }
        return "일반";
    }

    // 채팅방에서 사용할 형태로 파싱
    private List<Map<String, Object>> parseProblemsForChat(String problemJson) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(problemJson);
            ArrayNode problemsArray = (ArrayNode) root.get("problems");

            List<Map<String, Object>> result = new ArrayList<>();

            for (int i = 0; i < problemsArray.size(); i++) {
                ObjectNode problem = (ObjectNode) problemsArray.get(i);

                Map<String, Object> chatProblem = new HashMap<>();
                chatProblem.put("id", problem.get("id").asInt());
                chatProblem.put("question", problem.get("question").asText());

                // 선택지를 리스트로 변환
                ArrayNode optionsArray = (ArrayNode) problem.get("options");
                List<String> options = new ArrayList<>();
                for (int j = 0; j < optionsArray.size(); j++) {
                    options.add(optionsArray.get(j).asText());
                }
                chatProblem.put("options", options);
                chatProblem.put("correctAnswer", problem.get("correctAnswer").asInt());
                chatProblem.put("explanation", problem.get("explanation").asText());

                // 선택지별 설명이 있으면 추가
                if (problem.has("optionExplanations")) {
                    ArrayNode explanationsArray = (ArrayNode) problem.get("optionExplanations");
                    List<String> explanations = new ArrayList<>();
                    for (int j = 0; j < explanationsArray.size(); j++) {
                        explanations.add(explanationsArray.get(j).asText());
                    }
                    chatProblem.put("optionExplanations", explanations);
                }

                result.add(chatProblem);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("문제 파싱 실패", e);
        }
    }

    // 기존 채팅 세션의 문제들 조회
    public Map<String, Object> getChatSessionProblems(String chatSessionUuid) {
        try {
            List<Problem> problems = problemRepository.findByChatSessionUuidOrderByCreatedAtDesc(chatSessionUuid);

            List<Map<String, Object>> problemList = new ArrayList<>();
            for (Problem problem : problems) {
                Map<String, Object> problemData = new HashMap<>();
                problemData.put("problemUuid", problem.getUuid());
                problemData.put("createdAt", problem.getCreatedAt());
                problemData.put("problems", parseProblemsForChat(problem.getProblems()));
                problemList.add(problemData);
            }

            return Map.of(
                    "success", true,
                    "problems", problemList,
                    "count", problems.size(),
                    "chatSessionUuid", chatSessionUuid
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", "문제 조회 실패: " + e.getMessage()
            );
        }
    }

    // 답안 제출 처리
    public Map<String, Object> processAnswerSubmission(String problemUuid, Integer questionId, Integer selectedAnswer, String userUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) {
                return Map.of("success", false, "error", "문제를 찾을 수 없습니다.");
            }

            // 문제 JSON에서 정답 확인
            ObjectNode root = (ObjectNode) objectMapper.readTree(problem.getProblems());
            ArrayNode problemsArray = (ArrayNode) root.get("problems");

            for (int i = 0; i < problemsArray.size(); i++) {
                ObjectNode questionNode = (ObjectNode) problemsArray.get(i);
                if (questionNode.get("id").asInt() == questionId) {
                    int correctAnswer = questionNode.get("correctAnswer").asInt();
                    boolean isCorrect = correctAnswer == selectedAnswer;

                    String selectedOptionText = "";
                    String correctOptionText = "";

                    ArrayNode optionsArray = (ArrayNode) questionNode.get("options");
                    if (selectedAnswer < optionsArray.size()) {
                        selectedOptionText = optionsArray.get(selectedAnswer).asText();
                    }
                    if (correctAnswer < optionsArray.size()) {
                        correctOptionText = optionsArray.get(correctAnswer).asText();
                    }

                    return Map.of(
                            "success", true,
                            "isCorrect", isCorrect,
                            "selectedAnswer", selectedAnswer,
                            "selectedOptionText", selectedOptionText,
                            "correctAnswer", correctAnswer,
                            "correctOptionText", correctOptionText,
                            "explanation", questionNode.get("explanation").asText(),
                            "message", isCorrect ? "🎉 정답입니다!" : "❌ 틀렸습니다. 정답은 " + (correctAnswer + 1) + "번 '" + correctOptionText + "' 입니다."
                    );
                }
            }

            return Map.of("success", false, "error", "해당 문제를 찾을 수 없습니다.");

        } catch (Exception e) {
            return Map.of("success", false, "error", "답안 처리 실패: " + e.getMessage());
        }
    }

    // OpenAI 응답을 우리 형식으로 변환 (실제 AI 사용시)
    private String convertAIResponseToFormat(String aiResponse, Integer questionCount) {
        // TODO: 나중에 OpenAI 응답 파싱 로직 구현
        // 지금은 목업으로 대체
        System.out.println("OpenAI 응답: " + aiResponse);
        return generateMockProblemsFromChat("AI 생성 내용", questionCount);
    }
}