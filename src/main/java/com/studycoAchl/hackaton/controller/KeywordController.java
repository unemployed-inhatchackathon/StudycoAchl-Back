package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.service.KeywordExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
@Slf4j
public class KeywordController {

    private final KeywordExtractionService keywordExtractionService;

    /**
     * 채팅 세션에서 키워드 추출
     */
    @PostMapping("/extract/sessions/{sessionId}")
    public ApiResponse<List<String>> extractKeywordsFromSession(@PathVariable UUID sessionId) {
        try {
            log.info("채팅 세션 키워드 추출 요청 - sessionId: {}", sessionId);

            List<String> keywords = keywordExtractionService.extractKeywordsFromChatSession(sessionId);

            return ApiResponse.success(keywords, "채팅 세션에서 키워드를 추출했습니다.");

        } catch (Exception e) {
            log.error("채팅 세션 키워드 추출 실패 - sessionId: {}", sessionId, e);
            return ApiResponse.error("키워드 추출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 메시지에서 키워드 추출
     */
    @PostMapping("/extract/message")
    public ApiResponse<List<String>> extractKeywordsFromMessage(
            @RequestBody Map<String, String> request) {

        try {
            String messageContent = request.get("content");
            String subjectTitle = request.get("subject");

            if (messageContent == null || messageContent.trim().isEmpty()) {
                return ApiResponse.error("메시지 내용은 필수입니다.");
            }

            log.info("메시지 키워드 추출 요청 - subject: {}", subjectTitle);

            List<String> keywords = keywordExtractionService.extractKeywordsFromMessage(messageContent, subjectTitle);

            return ApiResponse.success(keywords, "메시지에서 키워드를 추출했습니다.");

        } catch (Exception e) {
            log.error("메시지 키워드 추출 실패", e);
            return ApiResponse.error("키워드 추출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅 세션의 현재 키워드 조회
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> getSessionKeywords(@PathVariable UUID sessionId) {
        try {
            // 세션의 키워드 정보를 Map으로 반환하도록 서비스 메소드 필요
            // 현재는 간단한 구현으로 대체
            List<String> keywords = keywordExtractionService.extractKeywordsFromChatSession(sessionId);

            Map<String, Object> keywordInfo = Map.of(
                    "sessionId", sessionId,
                    "keywords", keywords,
                    "keywordCount", keywords.size(),
                    "canGenerateProblems", keywords.size() >= 3
            );

            return ApiResponse.success(keywordInfo, "세션 키워드 정보를 조회했습니다.");

        } catch (Exception e) {
            log.error("세션 키워드 조회 실패 - sessionId: {}", sessionId, e);
            return ApiResponse.error("키워드 조회에 실패했습니다: " + e.getMessage());
        }
    }
}