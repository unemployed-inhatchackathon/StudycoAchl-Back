package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "material_quizzes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    @Column(name = "quiz_data", columnDefinition = "JSON")
    private String quizData; // JSON 형태의 문제들

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords; // 문제 생성에 사용된 키워드들

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "difficulty", length = 20)
    private String difficulty; // "쉬움", "보통", "어려움" -> 나중에 빼기

    @Column(name = "generation_method", length = 50)
    private String generationMethod; // "AI_SUMMARY", "KEYWORD_BASED", "FULL_TEXT"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_uuid", referencedColumnName = "uuid")
    private StudyMaterial studyMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    private AppUsers appUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    private Subject subject;

    // 편의 메서드
    public boolean hasQuizData() {
        return quizData != null && !quizData.trim().isEmpty();
    }

    // 팩토리 메서드
    public static MaterialQuiz createFromSummary(StudyMaterial material, String keywords,
                                                 String quizData, int questionCount) {
        return MaterialQuiz.builder()
                .studyMaterial(material)
                .appUsers(material.getAppUsers())
                .subject(material.getSubject())
                .keywords(keywords)
                .quizData(quizData)
                .questionCount(questionCount)
                .difficulty("보통")
                .generationMethod("AI_SUMMARY")
                .build();
    }

    public static MaterialQuiz createFromKeywords(StudyMaterial material, String keywords,
                                                  String quizData, int questionCount, String difficulty) {
        return MaterialQuiz.builder()
                .studyMaterial(material)
                .appUsers(material.getAppUsers())
                .subject(material.getSubject())
                .keywords(keywords)
                .quizData(quizData)
                .questionCount(questionCount)
                .difficulty(difficulty)
                .generationMethod("KEYWORD_BASED")
                .build();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (difficulty == null) {
            difficulty = "보통";
        }
        if (generationMethod == null) {
            generationMethod = "AI_SUMMARY";
        }
    }
}