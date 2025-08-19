package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "problems", columnDefinition = "JSON")
    private String problems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UUID", referencedColumnName = "UUID")
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UUID", referencedColumnName = "UUID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UUID", referencedColumnName = "UUID")
    private Subject subject;

    // 추가 필드들 (기존 코드 호환성을 위해)
    private String userUuid;
    private String subjectUuid;
    private String chatSessionUuid;
    private LocalDateTime createdData;

    // 편의 메서드
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdData = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdData;
    }
}