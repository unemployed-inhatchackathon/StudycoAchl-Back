package com.studycoAchl.hackaton.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitRequest {

    @NotNull(message = "문제 번호는 필수입니다.")
    @Min(value = 1, message = "문제 번호는 1 이상이어야 합니다.")
    private Integer questionNumber;

    @NotNull(message = "선택한 답안은 필수입니다.")
    @Min(value = 0, message = "선택한 답안은 0 이상이어야 합니다.")
    private Integer selectedAnswer;

    @NotNull(message = "사용자 UUID는 필수입니다.")
    private UUID userUuid;

    private Long timeSpentSeconds; // 소요 시간 (선택사항)
}