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
     * 채팅 세션 기반 문제 생성 (OpenAI 직접 사용)
     */
    public Map<String, Object> generateProblemsFromChatSession(
            String userUuid, String subjectUuid, String chatSessionUuid, int questionCount) {

        try {
            log.info("OpenAI로 문제 생성 시작 - sessionId: {}, count: {}", chatSessionUuid, questionCount);

            // 1. 채팅 세션에서 키워드 및 컨텍스트 추출
            List<String> keywords = extractKeywordsFromChatSession(chatSessionUuid);
            String context = extractContextFromChatSession(chatSessionUuid);

            log.info("추출된 키워드: {}", keywords);
            log.info("추출된 컨텍스트: {}", context.substring(0, Math.min(100, context.length())) + "...");

            // 2. OpenAI로 문제 생성
            String problemsJson = questionGenerationService.generateQuestionsJson(keywords, context, questionCount);

            // 3. 데이터베이스에 저장
            Problem problem = new Problem();
            problem.setUuid(UUID.randomUUID().toString());
            problem.setProblems(problemsJson);
            problem.setUserUuid(userUuid);
            problem.setSubjectUuid(subjectUuid);
            problem.setChatSessionUuid(chatSessionUuid);
            problem.setCreatedData(LocalDateTime.now());

            Problem savedProblem = problemRepository.save(problem);

            log.info("OpenAI 문제 생성 완료 - problemId: {}", savedProblem.getUuid());

            // 4. 응답 반환
            return Map.of(
                    "success", true,
                    "problemUuid", savedProblem.getUuid(),
                    "questionCount", questionCount,
                    "keywords", keywords,
                    "source", "OpenAI GPT-3.5-turbo",
                    "message", "AI가 채팅 내용을 분석해서 " + questionCount + "개의 맞춤 문제를 생성했습니다!",
                    "createdAt", savedProblem.getCreatedData()
            );

        } catch (Exception e) {
            log.error("OpenAI 문제 생성 실패 - sessionId: {}", chatSessionUuid, e);
            return Map.of(
                    "success", false,
                    "error", "AI 문제 생성 실패: " + e.getMessage(),
                    "errorDetail", "OpenAI API 호출 중 오류가 발생했습니다. API 키와 네트워크를 확인해주세요."
            );
        }
    }

    /**
     * 채팅 세션에서 키워드 추출
     */
    public List<String> extractKeywordsFromChatSession(String chatSessionUuid) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(chatSessionUuid);
            if (!sessionOpt.isPresent()) {
                throw new RuntimeException("채팅 세션을 찾을 수 없습니다: " + chatSessionUuid);
            }

            ChatSession session = sessionOpt.get();
            String messages = session.getMessages();

            if (messages == null || messages.isEmpty()) {
                log.warn("채팅 메시지가 비어있음, 과목 정보를 사용");
                return Arrays.asList("일반학습");
            }

            // 키워드 추출 로직
            List<String> keywords = new ArrayList<>();
            String text = messages.toLowerCase();

            // 과목별 키워드
            if (text.contains("수학") || text.contains("math") || text.contains("계산") ||
                    text.contains("공식") || text.contains("방정식")) {
                keywords.add("수학");
            }
            if (text.contains("영어") || text.contains("english") || text.contains("문법") ||
                    text.contains("단어") || text.contains("reading")) {
                keywords.add("영어");
            }
            if (text.contains("과학") || text.contains("science") || text.contains("물리") ||
                    text.contains("화학") || text.contains("생물")) {
                keywords.add("과학");
            }
            if (text.contains("역사") || text.contains("history") || text.contains("조선") ||
                    text.contains("전쟁")) {
                keywords.add("역사");
            }
            if (text.contains("국어") || text.contains("문학") || text.contains("시") ||
                    text.contains("소설")) {
                keywords.add("국어");
            }

            // 구체적인 토픽 키워드 추출
            if (text.contains("함수") || text.contains("그래프")) keywords.add("함수");
            if (text.contains("문법") || text.contains("시제")) keywords.add("문법");
            if (text.contains("역학") || text.contains("운동")) keywords.add("물리");

            // 키워드가 없으면 기본값
            if (keywords.isEmpty()) {
                keywords.add("종합학습");
            }

            return keywords;

        } catch (Exception e) {
            log.error("키워드 추출 실패", e);
            return Arrays.asList("일반");
        }
    }

    /**
     * 채팅 세션에서 컨텍스트(맥락) 추출
     */
    public String extractContextFromChatSession(String chatSessionUuid) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(chatSessionUuid);
            if (!sessionOpt.isPresent()) {
                return "일반적인 학습 내용";
            }

            ChatSession session = sessionOpt.get();
            String messages = session.getMessages();

            if (messages == null || messages.isEmpty()) {
                return "학습자와 AI 간의 대화 내용을 바탕으로 한 문제";
            }

            // 메시지가 너무 길면 요약
            if (messages.length() > 1000) {
                return messages.substring(0, 1000) + "... (대화 요약)";
            }

            return messages;

        } catch (Exception e) {
            log.error("컨텍스트 추출 실패", e);
            return "학습 관련 대화 내용";
        }
    }

    /**
     * 직접 키워드로 문제 생성
     */
    public Map<String, Object> generateProblemsFromKeywords(
            String userUuid, String subjectUuid, List<String> keywords,
            String context, int questionCount) {

        try {
            log.info("키워드 기반 OpenAI 문제 생성 - keywords: {}", keywords);

            String problemsJson = questionGenerationService.generateQuestionsJson(keywords, context, questionCount);

            Problem problem = new Problem();
            problem.setUuid(UUID.randomUUID().toString());
            problem.setProblems(problemsJson);
            problem.setUserUuid(userUuid);
            problem.setSubjectUuid(subjectUuid);
            problem.setChatSessionUuid(null); // 직접 생성이므로 null
            problem.setCreatedData(LocalDateTime.now());

            Problem savedProblem = problemRepository.save(problem);

            return Map.of(
                    "success", true,
                    "problemUuid", savedProblem.getUuid(),
                    "questionCount", questionCount,
                    "keywords", keywords,
                    "source", "OpenAI Direct",
                    "message", "키워드 기반으로 " + questionCount + "개의 문제가 생성되었습니다!"
            );

        } catch (Exception e) {
            log.error("키워드 기반 문제 생성 실패", e);
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }
}