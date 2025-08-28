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
public class ChatSessionResponseDto {
    private UUID uuid;
    private String chatTitle;
    private LocalDateTime createdData;

    // 키워드 관련 (필요한 경우만)
    private List<String> extractedKeywordsList;
    private Integer generatedProblemCount;

    // 참조 ID만 포함 (객체 전체 말고)
    private UUID userUuid;
    private UUID subjectUuid;

    // 메시지 관련 (요약 정보만)
    private Integer messageCount;
}