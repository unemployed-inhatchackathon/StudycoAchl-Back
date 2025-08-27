package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wrong_answer_note")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongAnswerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_answer_uuid", referencedColumnName = "uuid")
    private UserAnswer userAnswer; // 원본 틀린 답안

    // 문제 정보 (복습용으로 복사 저장)
    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "options", columnDefinition = "JSON")
    private String options; // JSON 배열로 선택지 저장

    @Column(name = "correct_answer")
    private Integer correctAnswer;

    @Column(name = "user_wrong_answer")
    private Integer userWrongAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "keyword", length = 100)
    private String keyword;

    // 복습 상태
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "is_mastered")
    @Builder.Default
    private Boolean isMastered = false;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 복습 관련 메소드
    public void markAsReviewed() {
        this.reviewCount = (reviewCount != null ? reviewCount : 0) + 1;
        this.lastReviewedAt = LocalDateTime.now();
    }

    public void markAsMastered() {
        this.isMastered = true;
        this.lastReviewedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (isMastered == null) {
            isMastered = false;
        }
    }
}