package com.studycoAchl.hackaton.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.studycoAchl.hackaton.dto.ChatMessage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "chatTitle", length = 200)
    private String chatTitle;

    // JSON으로 메시지 저장
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "JSON")
    private List<ChatMessage> messages;

    @Column(name = "created_data")
    private LocalDateTime createdData;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 문제 생성 기능을 위한 필드들 (당신 방식 유지)
    @Column(name = "extracted_keywords", columnDefinition = "TEXT")
    private String extractedKeywords;

    @Column(name = "last_keyword_extraction")
    private LocalDateTime lastKeywordExtraction;

    @Column(name = "generated_problem_count")
    private Integer generatedProblemCount;

    @Column(name = "last_problem_generation")
    private LocalDateTime lastProblemGeneration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SessionStatus status;

    // JPA 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    @JsonBackReference
    private AppUsers appUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    @JsonBackReference
    private Subject subject;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Problem> problems;

    public enum SessionStatus {
        ACTIVE, COMPLETED, PAUSED
    }

    // 생성자 (편의 메소드)
    public ChatSession(AppUsers appUsers, Subject subject, String title) {
        this();
        this.appUsers = appUsers;
        this.subject = subject;
        this.chatTitle = title;
    }

    // ========== 메시지 관련 메소드들 (ChatMessage DTO 사용) ==========

    /**
     * 메시지 추가 (기존 방식 유지)
     */
    public void addMessage(String sender, String content) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }

        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                sender,
                content,
                LocalDateTime.now()
        );

        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 교육적 내용 포함한 메시지 추가 (새로운 기능)
     */
    public ChatMessage addEducationalMessage(String sender, String content, boolean hasEducationalContent) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }

        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                sender,
                content,
                LocalDateTime.now(),
                hasEducationalContent
        );

        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();

        return message;
    }

    /**
     * 메시지 개수 조회
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    /**
     * 사용자 메시지만 조회
     */
    public List<ChatMessage> getUserMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages.stream()
                .filter(ChatMessage::isUserMessage)
                .collect(Collectors.toList());
    }

    /**
     * 교육적 내용이 포함된 메시지들만 조회
     */
    public List<ChatMessage> getEducationalMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages.stream()
                .filter(ChatMessage::hasEducationalContent)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 추출이 필요한 메시지들 조회
     */
    public List<ChatMessage> getUnprocessedMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages.stream()
                .filter(msg -> msg.hasEducationalContent() && !msg.isProcessedForKeywords())
                .collect(Collectors.toList());
    }

    // ========== 키워드 관련 메소드들 ==========

    /**
     * 세션 전체에서 키워드 추가
     */
    public void addExtractedKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        if (this.extractedKeywords == null || this.extractedKeywords.isEmpty()) {
            this.extractedKeywords = keyword.trim();
        } else {
            List<String> currentKeywords = getExtractedKeywordsList();
            if (!currentKeywords.contains(keyword.trim())) {
                this.extractedKeywords += "," + keyword.trim();
            }
        }
        this.lastKeywordExtraction = LocalDateTime.now();
    }

    /**
     * 키워드 리스트로 반환
     */
    public List<String> getExtractedKeywordsList() {
        if (extractedKeywords == null || extractedKeywords.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(extractedKeywords.split(",")).stream()
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 메시지에서 추출된 모든 키워드 수집
     */
    public List<String> getAllKeywordsFromMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(ChatMessage::hasExtractedKeywords)
                .flatMap(msg -> msg.getExtractedKeywordsList().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 문제 생성 가능 여부 확인
     */
    public boolean canGenerateProblems() {
        // 세션 키워드 또는 메시지 키워드 중 하나라도 3개 이상이면 가능
        List<String> sessionKeywords = getExtractedKeywordsList();
        List<String> messageKeywords = getAllKeywordsFromMessages();

        return sessionKeywords.size() >= 3 || messageKeywords.size() >= 3;
    }

    /**
     * 문제 생성용 상위 키워드 추출
     */
    public List<String> getTopKeywordsForProblemGeneration(int limit) {
        List<String> sessionKeywords = getExtractedKeywordsList();
        List<String> messageKeywords = getAllKeywordsFromMessages();

        // 세션 키워드 우선, 부족하면 메시지 키워드로 보완
        List<String> combinedKeywords = new ArrayList<>(sessionKeywords);

        for (String messageKeyword : messageKeywords) {
            if (!combinedKeywords.contains(messageKeyword) && combinedKeywords.size() < limit) {
                combinedKeywords.add(messageKeyword);
            }
        }

        return combinedKeywords.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ========== 기존 호환성 메소드들 ==========

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }

    public String getChatTitle() {
        return this.chatTitle;
    }

    public void setCreatedData(LocalDateTime createdData) {
        this.createdData = createdData;
    }

    public LocalDateTime getCreatedData() {
        return this.createdData;
    }

    public void incrementProblemCount() {
        if (this.generatedProblemCount == null) {
            this.generatedProblemCount = 0;
        }
        this.generatedProblemCount++;
        this.lastProblemGeneration = LocalDateTime.now();
    }

    // ========== JPA 생명주기 메소드들 ==========

    @PrePersist
    protected void onCreate() {
        if (createdData == null) {
            createdData = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (generatedProblemCount == null) {
            generatedProblemCount = 0;
        }
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (messages == null) {
            messages = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}