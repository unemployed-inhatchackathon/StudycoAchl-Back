package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStatsResponse {

    // 전체 학습 통계
    private Long totalQuizzes;
    private Double averageScore;
    private Long totalCorrectAnswers;
    private Long totalWrongAnswers;
    private Double overallAccuracy;

    // 최근 성과
    private List<RecentQuizResult> recentResults;
    private Boolean isImproving;
    private Integer recentScoreTrend; // +5, -2 등

    // 오답노트 통계
    private Long totalWrongAnswersInNote;
    private Long masteredWrongAnswers;
    private Long needsReviewCount;

    // 키워드별 취약점
    private List<KeywordWeakness> weakKeywords;
    private String strongestKeyword;
    private String weakestKeyword;

    // 학습 활동
    private LocalDateTime lastQuizDate;
    private LocalDateTime lastReviewDate;
    private Integer studyStreakDays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentQuizResult {
        private String subjectTitle;
        private Integer score;
        private LocalDateTime completedAt;
        private Boolean isPassed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordWeakness {
        private String keyword;
        private Long wrongCount;
        private Double weaknessLevel; // 0-100
        private Boolean needsUrgentReview;
    }
}