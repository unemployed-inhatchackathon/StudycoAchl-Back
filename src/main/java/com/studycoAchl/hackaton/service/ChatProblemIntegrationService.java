package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatProblemIntegrationService {

    private final ChatSessionService chatSessionService;
    private final KeywordExtractionService keywordExtractionService;
    private final ProblemGenerationService problemGenerationService;

    /**
     * 채팅 세션에서 키워드를 추출하여 문제 생성 (핵심 통합 메소드)
     */
    public Map<String, Object> generateProblemFromChat(UUID chatSessionId, int questionCount) {
        try {
            log.info("채팅 기반 문제 생성 시작 - sessionId: {}, questionCount: {}", chatSessionId, questionCount);

            // 1. 채팅 세션 유효성 확인
            ChatSession chatSession = chatSessionService.findById(chatSessionId);

            // 2. 채팅에서 키워드 추출
            List<String> extractedKeywords = keywordExtractionService.extractKeywordsFromChatSession(chatSessionId);

            if (extractedKeywords.isEmpty()) {
                return createErrorResponse("채팅에서 학습 관련 키워드를 찾을 수 없습니다.");
            }

            // 3. 키워드 기반 문제 생성
            Map<String, Object> problemResult = problemGenerationService.generateProblemsFromChatSession(
                    chatSession.getAppUsers().getUuid(),
                    chatSession.getSubject().getUuid(),
                    chatSessionId,
                    questionCount
            );

            // 4. 채팅 세션 업데이트 (문제 생성 카운트 증가)
            chatSessionService.incrementProblemCount(chatSessionId);

            // 5. 성공 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chatSessionId", chatSessionId);
            response.put("extractedKeywords", extractedKeywords);
            response.put("problemResult", problemResult);
            response.put("message", "채팅 내용을 바탕으로 " + questionCount + "개의 문제가 생성되었습니다!");
            response.put("generatedAt", LocalDateTime.now());

            log.info("채팅 기반 문제 생성 완료 - sessionId: {}", chatSessionId);
            return response;

        } catch (Exception e) {
            log.error("채팅 기반 문제 생성 실패 - sessionId: {}", chatSessionId, e);
            return createErrorResponse("문제 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 실시간 채팅 중 키워드 자동 추출
     */
    public Map<String, Object> analyzeMessageForKeywords(UUID chatSessionId, String messageContent) {
        try {
            log.debug("메시지 키워드 분석 - sessionId: {}", chatSessionId);

            ChatSession chatSession = chatSessionService.findById(chatSessionId);

            // 1. 메시지에서 키워드 추출
            List<String> messageKeywords = keywordExtractionService.extractKeywordsFromMessage(
                    messageContent,
                    chatSession.getSubject().getTitle()
            );

            // 2. 교육적 내용이 포함되어 있는지 판단
            boolean hasEducationalContent = isEducationalMessage(messageContent, messageKeywords);

            // 3. 교육적 내용이면 세션에 키워드 추가
            if (hasEducationalContent && !messageKeywords.isEmpty()) {
                for (String keyword : messageKeywords) {
                    chatSessionService.addExtractedKeyword(chatSessionId, keyword);
                }
            }

            // 4. 문제 생성 가능 여부 확인
            boolean canGenerateProblems = chatSession.canGenerateProblems();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("extractedKeywords", messageKeywords);
            response.put("hasEducationalContent", hasEducationalContent);
            response.put("canGenerateProblems", canGenerateProblems);
            response.put("totalSessionKeywords", chatSession.getExtractedKeywordsList().size());

            return response;

        } catch (Exception e) {
            log.error("메시지 키워드 분석 실패 - sessionId: {}", chatSessionId, e);
            return createErrorResponse("메시지 분석 실패: " + e.getMessage());
        }
    }

    /**
     * 문제 생성 가능 여부 확인
     */
    public boolean canGenerateProblemsFromChat(UUID chatSessionId) {
        try {
            ChatSession chatSession = chatSessionService.findById(chatSessionId);
            return chatSession.canGenerateProblems();

        } catch (Exception e) {
            log.error("문제 생성 가능성 확인 실패 - sessionId: {}", chatSessionId, e);
            return false;
        }
    }

    /**
     * 문제 생성용 키워드 조회
     */
    public List<String> getKeywordsForProblemGeneration(UUID chatSessionId) {
        try {
            ChatSession chatSession = chatSessionService.findById(chatSessionId);
            return chatSession.getTopKeywordsForProblemGeneration(10);

        } catch (Exception e) {
            log.error("문제 생성용 키워드 조회 실패 - sessionId: {}", chatSessionId, e);
            return List.of();
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    /**
     * 교육적 메시지 여부 판단
     */
    private boolean isEducationalMessage(String messageContent, List<String> keywords) {
        if (messageContent == null || messageContent.trim().length() < 10) {
            return false;
        }

        // 키워드가 있으면 교육적 내용일 가능성이 높음
        if (!keywords.isEmpty()) {
            return true;
        }

        // 질문 형태인지 확인
        if (messageContent.contains("?") || messageContent.contains("어떻게") ||
                messageContent.contains("무엇") || messageContent.contains("왜")) {
            return true;
        }

        return false;
    }
}