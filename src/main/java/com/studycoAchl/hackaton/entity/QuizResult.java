package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_uuid", referencedColumnName = "uuid")
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    private AppUsers appUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    private Subject subject;

    // 결과 데이터
    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "wrong_answers")
    private Integer wrongAnswers;

    @Column(name = "score")
    private Integer score; // 백분율 점수 (0-100)

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    // 상태 정보
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ResultStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 사용자 답안들
    @OneToMany(mappedBy = "quizResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserAnswer> userAnswers = new ArrayList<>();

    public enum ResultStatus {
        IN_PROGRESS, COMPLETED, ABANDONED
    }

    // 편의 메소드들
    public double getAccuracyRate() {
        if (totalQuestions == null || totalQuestions == 0) return 0.0;
        return (double) correctAnswers / totalQuestions * 100.0;
    }

    public void calculateScore() {
        if (totalQuestions != null && totalQuestions > 0) {
            this.score = (int) Math.round(getAccuracyRate());
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ResultStatus.IN_PROGRESS;
        }
    }
}