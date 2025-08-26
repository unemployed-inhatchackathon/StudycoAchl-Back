package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "problem")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "UUID", columnDefinition = "Binary(16)")
    private UUID uuid;

    @Column(name = "problems", columnDefinition = "JSON")
    private String problems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_uuid", referencedColumnName = "UUID")
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "UUID")
    private Subject subject;

    // 추가 필드들 (기존 코드 호환성을 위해) - UUID 타입으로 변경
    @Column(name = "user_uuid", insertable = false, updatable = false)
    private UUID userUuid;

    @Column(name = "subject_uuid", insertable = false, updatable = false)
    private UUID subjectUuid;

    @Column(name = "chat_session_uuid", insertable = false, updatable = false)
    private UUID chatSessionUuid;

    @Column(name = "created_data")
    private LocalDateTime createdData;

    // 편의 메서드
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdData = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdData;
    }
}