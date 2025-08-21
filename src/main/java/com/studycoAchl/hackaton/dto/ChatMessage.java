package com.studycoAchl.hackaton.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {

    private String id;
    private String sender; // "USER" or "AI"
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    // ========== 키워드 통합 기능을 위한 필드 ==========
    private Boolean containsEducationalContent; // 교육적 내용 포함 여부
    private String extractedKeywords; // 추출된 키워드들 (콤마 구분)
    private Boolean processedForKeywords; // 키워드 처리 완료 여부

    // ========== 기존 호환성 생성자들 ==========

    public ChatMessage(String id, String sender, String content, LocalDateTime sentAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
        this.containsEducationalContent = false;
        this.processedForKeywords = false;
    }

    public ChatMessage(String id, String sender, String content, LocalDateTime sentAt,
                       Boolean containsEducationalContent) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
        this.containsEducationalContent = containsEducationalContent;
        this.processedForKeywords = false;
    }

    // ========== 핵심 편의 메소드들 ==========

    /**
     * 사용자 메시지인지 확인
     */
    public boolean isUserMessage() {
        return "USER".equalsIgnoreCase(sender) || "user".equalsIgnoreCase(sender);
    }

    /**
     * AI 메시지인지 확인
     */
    public boolean isAiMessage() {
        return "AI".equalsIgnoreCase(sender) || "ai".equalsIgnoreCase(sender);
    }

    /**
     * 교육적 내용 포함 여부
     */
    public boolean hasEducationalContent() {
        return containsEducationalContent != null && containsEducationalContent;
    }

    /**
     * 키워드가 추출되었는지 확인
     */
    public boolean hasExtractedKeywords() {
        return extractedKeywords != null && !extractedKeywords.trim().isEmpty();
    }

    /**
     * 키워드 처리가 완료되었는지 확인
     */
    public boolean isProcessedForKeywords() {
        return processedForKeywords != null && processedForKeywords;
    }

    /**
     * 추출된 키워드를 리스트로 반환
     */
    public List<String> getExtractedKeywordsList() {
        if (extractedKeywords == null || extractedKeywords.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(extractedKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }
}