package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.studycoAchl.hackaton.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ProblemGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ProblemGenerationService.class);

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private QuestionGenerationService questionGenerationService;

    /**
     * 키워드 기반 문제 생성 (직접 키워드 입력)
     */
    public Map<String, Object> generateProblemsFromKeywords(
            String userUuid, String subjectUuid, List<String> keywords,
            String context, int questionCount) {

        try {
            log.info("키워드 기반 문제 생성 시작 - keywords: {}, count: {}", keywords, questionCount);

            // OpenAI로 문제 생성
            String problemsJson = questionGenerationService.generateQuestionsJson(keywords, context, questionCount);

            // 데이터베이스에 저장
            Problem problem = new Problem();
            problem.setUuid(UUID.randomUUID().toString());
            problem.setProblems(problemsJson);
            problem.setUserUuid(userUuid);
            problem.setSubjectUuid(subjectUuid);
            problem.setChatSessionUuid(null); // 직접 생성이므로 null
            problem.setCreatedData(LocalDateTime.now());

            Problem savedProblem = problemRepository.save(problem);

            log.info("키워드 기반 문제 생성 완료 - problemId: {}", savedProblem.getUuid());

            return Map.of(
                    "success", true,
                    "problemUuid", savedProblem.getUuid(),
                    "questionCount", questionCount,
                    "keywords", keywords,
                    "source", "OpenAI GPT-3.5-turbo",
                    "message", "키워드 기반으로 " + questionCount + "개의 문제가 생성되었습니다!",
                    "createdAt", savedProblem.getCreatedData()
            );

        } catch (Exception e) {
            log.error("키워드 기반 문제 생성 실패", e);
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 채팅 세션 기반 문제 생성 (현재는 기본 키워드 사용)
     */
    public Map<String, Object> generateProblemsFromChatSession(
            String userUuid, String subjectUuid, String chatSessionUuid, int questionCount) {

        try {
            log.info("채팅 세션 기반 문제 생성 시작 - sessionId: {}, count: {}", chatSessionUuid, questionCount);

            // 채팅 세션 확인
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(chatSessionUuid);
            if (!sessionOpt.isPresent()) {
                throw new RuntimeException("채팅 세션을 찾을 수 없습니다: " + chatSessionUuid);
            }

            // 현재는 ChatMessage가 없으므로 기본 키워드 사용
            List<String> keywords = getDefaultKeywords();
            String context = "일반적인 학습 내용을 바탕으로 한 문제";

            log.info("기본 키워드 사용: {}", keywords);

            // OpenAI로 문제 생성
            String problemsJson = questionGenerationService.generateQuestionsJson(keywords, context, questionCount);

            // 데이터베이스에 저장
            Problem problem = new Problem();
            problem.setUuid(UUID.randomUUID().toString());
            problem.setProblems(problemsJson);
            problem.setUserUuid(userUuid);
            problem.setSubjectUuid(subjectUuid);
            problem.setChatSessionUuid(chatSessionUuid);
            problem.setCreatedData(LocalDateTime.now());

            Problem savedProblem = problemRepository.save(problem);

            // 세션의 문제 생성 카운트 증가
            updateSessionProblemGeneration(chatSessionUuid);

            log.info("채팅 세션 기반 문제 생성 완료 - problemId: {}", savedProblem.getUuid());

            return Map.of(
                    "success", true,
                    "problemUuid", savedProblem.getUuid(),
                    "questionCount", questionCount,
                    "keywords", keywords,
                    "source", "OpenAI GPT-3.5-turbo",
                    "message", "AI가 " + questionCount + "개의 문제를 생성했습니다!",
                    "createdAt", savedProblem.getCreatedData(),
                    "note", "ChatMessage 구현 후 채팅 내용 기반 문제 생성이 가능합니다."
            );

        } catch (Exception e) {
            log.error("채팅 세션 기반 문제 생성 실패 - sessionId: {}", chatSessionUuid, e);
            return Map.of(
                    "success", false,
                    "error", "AI 문제 생성 실패: " + e.getMessage(),
                    "errorDetail", "OpenAI API 호출 중 오류가 발생했습니다. API 키와 네트워크를 확인해주세요."
            );
        }
    }

    /**
     * 기본 키워드 제공
     */
    private List<String> getDefaultKeywords() {
        return Arrays.asList("일반학습", "종합문제", "기본지식");
    }

    /**
     * 과목별 기본 키워드 제공
     */
    public List<String> getKeywordsBySubject(String subjectUuid) {
        List<String> keywords = new ArrayList<>();

        if (subjectUuid != null) {
            // Subject 정보를 통해 키워드 결정 (현재는 간단한 패턴 매칭)
            String subjectLower = subjectUuid.toLowerCase();

            if (subjectLower.contains("math") || subjectLower.contains("수학")) {
                keywords.addAll(Arrays.asList("수학", "계산", "공식", "방정식", "함수", "그래프"));
            } else if (subjectLower.contains("english") || subjectLower.contains("영어")) {
                keywords.addAll(Arrays.asList("영어", "문법", "단어", "독해", "회화", "시제"));
            } else if (subjectLower.contains("science") || subjectLower.contains("과학")) {
                keywords.addAll(Arrays.asList("과학", "물리", "화학", "생물", "실험", "이론"));
            } else if (subjectLower.contains("programming") || subjectLower.contains("프로그래밍")) {
                keywords.addAll(Arrays.asList("프로그래밍", "자바", "알고리즘", "데이터베이스", "개발", "코딩"));
            } else if (subjectLower.contains("history") || subjectLower.contains("역사")) {
                keywords.addAll(Arrays.asList("역사", "조선", "고려", "전쟁", "문화", "정치"));
            } else {
                keywords.addAll(getDefaultKeywords());
            }
        } else {
            keywords.addAll(getDefaultKeywords());
        }

        return keywords;
    }

    /**
     * 문제 생성 미리보기 (비용 절약을 위한 샘플)
     */
    public Map<String, Object> previewProblems(
            List<String> keywords, String context, int questionCount) {

        try {
            log.info("문제 미리보기 생성 - keywords: {}, count: {}", keywords, questionCount);

            // OpenAI 대신 샘플 문제 반환
            List<Map<String, Object>> sampleQuestions = new ArrayList<>();

            for (int i = 0; i < Math.min(questionCount, 3); i++) { // 최대 3개만 미리보기
                Map<String, Object> question = new HashMap<>();
                question.put("id", i + 1);
                question.put("question", String.join(", ", keywords) + " 관련 문제 " + (i + 1) + ": 다음 중 올바른 설명은?");
                question.put("options", Arrays.asList(
                        "첫 번째 선택지 (정답)",
                        "두 번째 선택지",
                        "세 번째 선택지",
                        "네 번째 선택지"
                ));
                question.put("correctAnswer", 0);
                question.put("difficulty", estimateDifficultyFromKeywords(keywords));
                question.put("timeLimit", 45);
                question.put("keyword", String.join(", ", keywords));
                question.put("hint", "키워드: " + String.join(", ", keywords));
                question.put("explanation", "이것은 " + String.join(", ", keywords) + " 관련 문제의 샘플입니다.");
                sampleQuestions.add(question);
            }

            return Map.of(
                    "success", true,
                    "isPreview", true,
                    "sampleQuestions", sampleQuestions,
                    "totalRequestedCount", questionCount,
                    "previewCount", sampleQuestions.size(),
                    "keywords", keywords,
                    "message", "문제 미리보기입니다. 실제 생성 시 " + questionCount + "개의 문제가 생성됩니다."
            );

        } catch (Exception e) {
            log.error("문제 미리보기 생성 실패", e);
            return Map.of(
                    "success", false,
                    "error", "문제 미리보기 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 키워드 기반 난이도 추정
     */
    private String estimateDifficultyFromKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "보통";
        }

        // 고급 키워드들
        List<String> advancedKeywords = Arrays.asList(
                "미적분", "고급수학", "유기화학", "양자역학", "알고리즘", "데이터구조",
                "심화", "고급", "복잡한", "전문", "상급"
        );

        // 초급 키워드들
        List<String> beginnerKeywords = Arrays.asList(
                "기초", "기본", "초급", "입문", "간단한", "쉬운", "시작"
        );

        long advancedCount = keywords.stream()
                .mapToLong(keyword -> advancedKeywords.stream()
                        .mapToLong(advanced -> keyword.toLowerCase().contains(advanced.toLowerCase()) ? 1 : 0)
                        .sum())
                .sum();

        long beginnerCount = keywords.stream()
                .mapToLong(keyword -> beginnerKeywords.stream()
                        .mapToLong(beginner -> keyword.toLowerCase().contains(beginner.toLowerCase()) ? 1 : 0)
                        .sum())
                .sum();

        if (advancedCount > beginnerCount) {
            return "어려움";
        } else if (beginnerCount > advancedCount) {
            return "쉬움";
        } else {
            return "보통";
        }
    }

    /**
     * 세션의 문제 생성 정보 업데이트
     */
    private void updateSessionProblemGeneration(String chatSessionUuid) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(chatSessionUuid);
            if (sessionOpt.isPresent()) {
                ChatSession session = sessionOpt.get();
                session.incrementProblemCount();
                chatSessionRepository.save(session);
                log.info("세션 문제 생성 카운트 업데이트 완료 - sessionId: {}", chatSessionUuid);
            }
        } catch (Exception e) {
            log.warn("세션 문제 생성 카운트 업데이트 실패 - sessionId: {}", chatSessionUuid, e);
        }
    }

    /**
     * 문제 데이터 검증
     */
    public Map<String, Object> validateProblemData(String problemsJson) {
        try {
            if (problemsJson == null || problemsJson.isEmpty()) {
                return Map.of(
                        "isValid", false,
                        "error", "문제 데이터가 비어있습니다."
                );
            }

            // JSON 형식 검증
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(problemsJson);

                // 필수 필드 확인
                if (!rootNode.has("questions")) {
                    return Map.of(
                            "isValid", false,
                            "error", "questions 필드가 없습니다."
                    );
                }

                com.fasterxml.jackson.databind.JsonNode questionsArray = rootNode.get("questions");
                if (!questionsArray.isArray() || questionsArray.size() == 0) {
                    return Map.of(
                            "isValid", false,
                            "error", "유효한 문제가 없습니다."
                    );
                }

                // 각 문제의 필수 필드 확인
                for (com.fasterxml.jackson.databind.JsonNode question : questionsArray) {
                    if (!question.has("question") || !question.has("options") || !question.has("correctAnswer")) {
                        return Map.of(
                                "isValid", false,
                                "error", "문제에 필수 필드가 누락되었습니다."
                        );
                    }
                }

            } catch (Exception e) {
                return Map.of(
                        "isValid", false,
                        "error", "유효하지 않은 JSON 형식입니다."
                );
            }

            return Map.of(
                    "isValid", true,
                    "message", "문제 데이터가 유효합니다."
            );

        } catch (Exception e) {
            log.error("문제 데이터 검증 실패", e);
            return Map.of(
                    "isValid", false,
                    "error", "검증 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 키워드 추천 기능
     */
    public List<String> recommendKeywords(String subjectUuid, String difficulty) {
        List<String> baseKeywords = getKeywordsBySubject(subjectUuid);
        List<String> recommendedKeywords = new ArrayList<>(baseKeywords);

        // 난이도에 따른 추가 키워드
        if ("쉬움".equals(difficulty)) {
            recommendedKeywords.addAll(Arrays.asList("기초", "입문", "기본개념"));
        } else if ("어려움".equals(difficulty)) {
            recommendedKeywords.addAll(Arrays.asList("심화", "고급", "응용문제"));
        } else {
            recommendedKeywords.addAll(Arrays.asList("표준", "일반", "중급"));
        }

        // 중복 제거 및 최대 10개로 제한
        return recommendedKeywords.stream()
                .distinct()
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 문제 생성 통계
     */
    public Map<String, Object> getProblemGenerationStats(String userUuid) {
        try {
            // 사용자의 총 문제 생성 수
            List<Problem> userProblems = problemRepository.findByUserUuid(userUuid);

            // 최근 생성된 문제들
            List<Problem> recentProblems = userProblems.stream()
                    .sorted((p1, p2) -> p2.getCreatedData().compareTo(p1.getCreatedData()))
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());

            return Map.of(
                    "totalProblems", userProblems.size(),
                    "recentProblems", recentProblems.size(),
                    "lastGeneratedAt", userProblems.isEmpty() ? null :
                            userProblems.stream()
                                    .map(Problem::getCreatedData)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null)
            );

        } catch (Exception e) {
            log.error("문제 생성 통계 조회 실패 - userUuid: {}", userUuid, e);
            return Map.of(
                    "error", "통계 조회에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 키워드 빈도 분석
     */
    public Map<String, Long> analyzeKeywordFrequency(List<String> keywords) {
        return keywords.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        keyword -> keyword,
                        java.util.stream.Collectors.counting()
                ));
    }

    /**
     * 특정 키워드의 빈도 계산
     */
    private long countKeywordFrequency(String text, String keyword) {
        if (text == null || keyword == null) {
            return 0;
        }

        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        long count = 0;
        int index = 0;

        while ((index = lowerText.indexOf(lowerKeyword, index)) != -1) {
            count++;
            index += lowerKeyword.length();
        }

        return count;
    }
}