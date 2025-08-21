package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubject {

    @NotBlank(message = "과목명은 필수입니다.")
    @Size(max = 50, message = "과목명은 50자를 초과할 수 없습니다.")
    private String title;  // name → title로 변경 (엔티티와 일치)
}