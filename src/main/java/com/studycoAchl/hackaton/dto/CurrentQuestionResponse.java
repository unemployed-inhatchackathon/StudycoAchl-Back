package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentQuestionResponse {

    // ========== 핵심 문제 정보 ==========
    private String questionId;           // 문제 식별자
    private String questionText;         // 문제 내용
    private List<String> options;        // 선택지들

    // ========== 진행 상황 정보 ==========
    private int questionNumber;          // 현재 문제 번호 (1, 2, 3...)
    private int totalQuestions;          // 전체 문제 수

    // ========== 메타 정보 ==========
    private String difficulty;           // 난이도
    private String category;             // 카테고리 (과목명)
    private Integer timeLimit;           // 시간 제한 (초)
    private Boolean hasHint;             // 힌트 존재 여부

    // ========== 세션 정보 ==========
    private LocalDateTime questionStartTime; // 문제 시작 시간
}