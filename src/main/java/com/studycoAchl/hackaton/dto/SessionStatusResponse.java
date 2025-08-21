package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {

    // ========== 세션 기본 정보 ==========
    private String sessionId;           // 세션 식별자
    private String status;               // WAITING, IN_PROGRESS, COMPLETED

    // ========== 진행 상황 정보 ==========
    private int currentQuestionNumber;   // 현재 문제 번호
    private int totalQuestions;          // 전체 문제 수

    // ========== 참가자 정보 ==========
    private Integer participantCount;    // 참가자 수

    // ========== 세션 메타 정보 ==========
    private LocalDateTime startedAt;     // 세션 시작 시간
    private String subjectTitle;         // 과목명
}