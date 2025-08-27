package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {

    // 기본 정보
    private UUID quizResultUuid;
    private UUID problemUuid;
    private String subjectTitle;

    // 점수 정보
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Integer score; // 0-100점
    private Double accuracyRate; // 정답률

    // 시간 정보
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer timeTakenMinutes;

    // 상태 정보
    private String status;
    private Boolean hasWrongAnswers;

    // 오답 관련 정보
    private Integer wrongAnswersInNote; // 오답노트에 추가된 문제 수
    private List<String> weakKeywords; // 취약한 키워드들

    // 메시지
    private String resultMessage;
    private String encouragementMessage;
}