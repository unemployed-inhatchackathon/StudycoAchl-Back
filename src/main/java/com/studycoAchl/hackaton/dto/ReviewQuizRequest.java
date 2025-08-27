package com.studycoAchl.hackaton.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQuizRequest {

    @NotNull(message = "사용자 UUID는 필수입니다.")
    private UUID userUuid;

    @NotNull(message = "과목 UUID는 필수입니다.")
    private UUID subjectUuid;

    @Min(value = 1, message = "최소 1개 문제는 생성해야 합니다.")
    @Max(value = 20, message = "최대 20개 문제까지 생성 가능합니다.")
    private Integer maxQuestions = 10;

    private String reviewType = "ALL"; // ALL, RECENT, DIFFICULT
}