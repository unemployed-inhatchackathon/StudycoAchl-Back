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
    private String questionId;           // UUID 형태
    private String questionText;         // 문제 내용
    private List<String> options;        // 5지선다 옵션
    private int questionNumber;          // 현재 문제 번호 (1, 2, 3...)
    private int totalQuestions;          // 전체 문제 수
    private String difficulty;           // 난이도
    private String category;             // 카테고리 (subject의 title)
    private Integer timeLimit;           // 시간 제한 (초)
    private LocalDateTime questionStartTime; // 문제 시작 시간
    private boolean hasHint;            // 힌트 존재 여부
}