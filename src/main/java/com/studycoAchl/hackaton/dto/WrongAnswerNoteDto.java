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
public class WrongAnswerNoteDto {

    private UUID wrongAnswerNoteUuid;
    private String questionText;
    private List<String> options;
    private Integer correctAnswer;
    private Integer userWrongAnswer;
    private String explanation;
    private String keyword;

    // 복습 상태
    private Integer reviewCount;
    private Boolean isMastered;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime createdAt;

    // 표시용 정보
    private String correctAnswerText;
    private String userWrongAnswerText;
    private String masteryStatus; // "숙지", "복습중", "미복습"
}