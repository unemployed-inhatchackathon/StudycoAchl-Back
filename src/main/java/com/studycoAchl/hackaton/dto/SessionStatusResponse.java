package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {
    private String sessionId;
    private String status;              // WAITING, IN_PROGRESS, COMPLETED
    private int currentQuestionNumber;
    private int totalQuestions;
    private int participantCount;
    private LocalDateTime startedAt;
    private String subjectTitle;        // 과목명
}