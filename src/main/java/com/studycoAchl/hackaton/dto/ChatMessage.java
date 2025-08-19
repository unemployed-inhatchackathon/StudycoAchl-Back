package com.studycoAchl.hackaton.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    private String id;
    private String sender; // "USER" or "AI"
    private String content;
    private String imageUrl; // 선택적, 이미지 첨부시 사용

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    // ========== 키워드 추출 기능을 위한 새 필드들 ==========
    private Boolean containsEducationalContent; // 교육적 내용 포함 여부
    private String extractedKeywords; // 이 메시지에서 추출된 키워드들 (콤마 구분)
    private Boolean processedForKeywords; // 키워드 추출 처리 완료 여부

    // ========== 기본 생성자 ==========
    public ChatMessage() {}

    // ========== 기존 생성자들 (팀원 호환성) ==========
    public ChatMessage(String id, String sender, String content, LocalDateTime sentAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
        this.containsEducationalContent = false;
        this.processedForKeywords = false;
    }

    public ChatMessage(String id, String sender, String content, String imageUrl, LocalDateTime sentAt) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.imageUrl = imageUrl;
        this.sentAt = sentAt;
        this.containsEducationalContent = false;
        this.processedForKeywords = false;
    }

    // ========== 새로운 생성자 (키워드 기능 포함) ==========
    public ChatMessage(String id, String sender, String content, LocalDateTime sentAt,
                       Boolean containsEducationalContent) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
        this.containsEducationalContent = containsEducationalContent;
        this.processedForKeywords = false;
    }

    // ========== 기존 Getters and Setters ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    // ========== 새로운 Getters and Setters (키워드 기능) ==========

    public Boolean getContainsEducationalContent() {
        return containsEducationalContent;
    }

    public void setContainsEducationalContent(Boolean containsEducationalContent) {
        this.containsEducationalContent = containsEducationalContent;
    }

    public String getExtractedKeywords() {
        return extractedKeywords;
    }

    public void setExtractedKeywords(String extractedKeywords) {
        this.extractedKeywords = extractedKeywords;
    }

    public Boolean getProcessedForKeywords() {
        return processedForKeywords;
    }

    public void setProcessedForKeywords(Boolean processedForKeywords) {
        this.processedForKeywords = processedForKeywords;
    }

    // ========== 편의 메소드들 ==========

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

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
     * 교육적 내용 포함 여부 (null 체크 포함)
     */
    public boolean hasEducationalContent() {
        return containsEducationalContent != null && containsEducationalContent;
    }

    /**
     * 키워드 처리 완료 여부 (null 체크 포함)
     */
    public boolean isProcessedForKeywords() {
        return processedForKeywords != null && processedForKeywords;
    }

    /**
     * 키워드가 추출되었는지 확인
     */
    public boolean hasExtractedKeywords() {
        return extractedKeywords != null && !extractedKeywords.trim().isEmpty();
    }

    /**
     * 추출된 키워드를 리스트로 반환
     */
    public java.util.List<String> getExtractedKeywordsList() {
        if (extractedKeywords == null || extractedKeywords.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return java.util.Arrays.stream(extractedKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
    }

    /**
     * 키워드 추가
     */
    public void addKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        if (extractedKeywords == null || extractedKeywords.isEmpty()) {
            extractedKeywords = keyword.trim();
        } else {
            // 중복 방지
            java.util.List<String> currentKeywords = getExtractedKeywordsList();
            if (!currentKeywords.contains(keyword.trim())) {
                extractedKeywords += "," + keyword.trim();
            }
        }
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", sentAt=" + sentAt +
                ", containsEducationalContent=" + containsEducationalContent +
                ", extractedKeywords='" + extractedKeywords + '\'' +
                ", processedForKeywords=" + processedForKeywords +
                '}';
    }
}