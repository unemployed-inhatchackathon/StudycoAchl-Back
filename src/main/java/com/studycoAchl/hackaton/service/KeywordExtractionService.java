package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.entity.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KeywordExtractionService {

    private final AiService aiService;
    private final ChatSessionService chatSessionService;

    /**
     * 채팅 세션에서 키워드 추출 (핵심 메소드)
     */
    public List<String> extractKeywordsFromChatSession(UUID sessionId) {
        try {
            log.info("채팅 세션에서 키워드 추출 시작 - sessionId: {}", sessionId);

            // 1. 채팅 세션 조회
            ChatSession chatSession = chatSessionService.findById(sessionId);

            // 2. 채팅 메시지들 수집
            List<String> messageContents = chatSessionService.getMessageContents(sessionId);

            if (messageContents.isEmpty()) {
                log.warn("추출할 메시지가 없음 - sessionId: {}", sessionId);
                return getDefaultKeywords(chatSession.getSubject());
            }

            // 3. 메시지들을 하나의 텍스트로 결합
            String conversationText = String.join(" ", messageContents);

            // 4. 과목 정보 가져오기
            String subjectTitle = chatSession.getSubject().getTitle();

            // 5. AI를 통한 키워드 추출
            String extractedKeywords = aiService.extractKeywords(conversationText, subjectTitle);

            // 6. 키워드 정제 및 검증
            List<String> cleanedKeywords = cleanAndValidateKeywords(extractedKeywords, subjectTitle);

            // 7. 채팅 세션에 키워드 저장
            saveKeywordsToSession(chatSession, cleanedKeywords);

            log.info("키워드 추출 완료 - sessionId: {}, keywords: {}", sessionId, cleanedKeywords);
            return cleanedKeywords;

        } catch (Exception e) {
            log.error("키워드 추출 실패 - sessionId: {}", sessionId, e);
            // 실패 시 기본 키워드 반환
            ChatSession chatSession = chatSessionService.findById(sessionId);
            return getDefaultKeywords(chatSession.getSubject());
        }
    }

    /**
     * 특정 메시지에서 키워드 추출
     */
    public List<String> extractKeywordsFromMessage(String messageContent, String subjectTitle) {
        try {
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return getDefaultKeywordsBySubject(subjectTitle);
            }

            // AI를 통한 키워드 추출
            String extractedKeywords = aiService.extractKeywords(messageContent, subjectTitle);

            // 키워드 정제 및 검증
            return cleanAndValidateKeywords(extractedKeywords, subjectTitle);

        } catch (Exception e) {
            log.error("메시지 키워드 추출 실패", e);
            return getDefaultKeywordsBySubject(subjectTitle);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * AI에서 추출된 키워드 정제 및 검증
     */
    private List<String> cleanAndValidateKeywords(String extractedKeywords, String subjectTitle) {
        if (extractedKeywords == null || extractedKeywords.trim().isEmpty()) {
            return getDefaultKeywordsBySubject(subjectTitle);
        }

        // 쉼표로 분할하고 정제
        List<String> keywords = Arrays.stream(extractedKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .filter(keyword -> keyword.length() >= 2) // 너무 짧은 키워드 제외
                .filter(keyword -> keyword.length() <= 20) // 너무 긴 키워드 제외
                .distinct()
                .limit(10) // 최대 10개로 제한
                .collect(Collectors.toList());

        // 키워드가 없으면 기본 키워드 반환
        if (keywords.isEmpty()) {
            return getDefaultKeywordsBySubject(subjectTitle);
        }

        return keywords;
    }

    /**
     * 채팅 세션에 키워드 저장
     */
    private void saveKeywordsToSession(ChatSession chatSession, List<String> keywords) {
        String keywordsString = String.join(",", keywords);
        chatSession.setExtractedKeywords(keywordsString);
        chatSession.setLastKeywordExtraction(LocalDateTime.now());
        chatSessionService.save(chatSession);
    }

    /**
     * 과목별 기본 키워드 반환
     */
    private List<String> getDefaultKeywordsBySubject(String subjectTitle) {
        if (subjectTitle == null) {
            return List.of("일반학습", "기본개념", "학습내용");
        }

        String subject = subjectTitle.toLowerCase();

        if (subject.contains("수학") || subject.contains("math")) {
            return List.of("수학", "계산", "공식", "문제해결");
        } else if (subject.contains("영어") || subject.contains("english")) {
            return List.of("영어", "문법", "어휘", "독해");
        } else if (subject.contains("과학") || subject.contains("science")) {
            return List.of("과학", "실험", "이론", "원리");
        } else if (subject.contains("프로그래밍") || subject.contains("programming")) {
            return List.of("프로그래밍", "코딩", "알고리즘", "개발");
        } else if (subject.contains("역사") || subject.contains("history")) {
            return List.of("역사", "사건", "인물", "문화");
        } else {
            return List.of("학습", "개념", "이해", "문제");
        }
    }

    /**
     * Subject 객체에서 기본 키워드 반환
     */
    private List<String> getDefaultKeywords(Subject subject) {
        if (subject == null) {
            return List.of("일반학습", "기본개념", "학습내용");
        }
        return getDefaultKeywordsBySubject(subject.getTitle());
    }
}