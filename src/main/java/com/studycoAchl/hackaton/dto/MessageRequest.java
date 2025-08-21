package com.studycoAchl.hackaton.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    @NotBlank(message = "발신자는 필수입니다.")
    @Pattern(regexp = "^(USER|AI|user|ai)$", message = "발신자는 USER 또는 AI여야 합니다.")
    private String sender;    // "USER" 또는 "AI"

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;   // 메시지 내용

    private String imageUrl;  // 이미지 URL (선택사항)
}