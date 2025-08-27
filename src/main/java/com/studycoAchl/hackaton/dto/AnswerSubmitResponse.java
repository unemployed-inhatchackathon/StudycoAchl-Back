package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitResponse {

    // 채점 결과
    private Boolean isCorrect;
    private Integer correctAnswer;
    private Integer selectedAnswer;
    private String explanation;
    private String keyword;

    // 진행 정보
    private Integer currentQuestionNumber;
    private Integer totalQuestions;
    private Boolean hasNextQuestion;
    private Boolean isQuizCompleted;

    // 오답노트 정보
    private Boolean addedToWrongAnswerNote;
    private UUID wrongAnswerNoteUuid;

    // 메시지
    private String resultMessage;
    private String nextActionMessage;
}