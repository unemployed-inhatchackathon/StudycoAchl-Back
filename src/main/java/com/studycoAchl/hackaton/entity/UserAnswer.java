package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_answer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_result_uuid", referencedColumnName = "uuid")
    private QuizResult quizResult;

    // 문제 정보
    @Column(name = "question_id")
    private String questionId; // Problem JSON 내의 문제 ID

    @Column(name = "question_number")
    private Integer questionNumber;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    // 답안 정보
    @Column(name = "selected_answer")
    private Integer selectedAnswer; // 사용자가 선택한 답 (0-based index)

    @Column(name = "correct_answer")
    private Integer correctAnswer; // 정답 (0-based index)

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // 메타데이터
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "keyword", length = 100)
    private String keyword; // 문제의 키워드

    @PrePersist
    protected void onCreate() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
        if (isCorrect == null && selectedAnswer != null && correctAnswer != null) {
            isCorrect = selectedAnswer.equals(correctAnswer);
        }
    }
}