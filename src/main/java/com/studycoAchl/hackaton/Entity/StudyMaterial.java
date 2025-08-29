package com.studycoAchl.hackaton.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "study_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "processing_status")
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @Column(name = "summary_generated_at")
    private LocalDateTime summaryGeneratedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    private AppUsers appUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    private Subject subject;

    // MaterialQuiz와의 관계
    @OneToMany(mappedBy = "studyMaterial", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<MaterialQuiz> materialQuizzes = new ArrayList<>();

    // 처리 상태 enum
    public enum ProcessingStatus {
        UPLOADED,       // 업로드 완료
        EXTRACTING,     // 텍스트 추출 중
        SUMMARIZING,    // 요약 생성 중
        COMPLETED,      // 처리 완료
        FAILED          // 처리 실패
    }

    // 편의 메서드
    public boolean isProcessingCompleted() {
        return ProcessingStatus.COMPLETED.equals(processingStatus);
    }

    public boolean hasSummary() {
        return aiSummary != null && !aiSummary.trim().isEmpty();
    }

    public void updateSummary(String summary) {
        this.aiSummary = summary;
        this.summaryGeneratedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void setProcessingFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    // 팩토리 메서드
    public static StudyMaterial createPdfMaterial(String fileName, String filePath,
                                                  Long fileSize, AppUsers user, Subject subject) {
        return StudyMaterial.builder()
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .appUsers(user)
                .subject(subject)
                .processingStatus(ProcessingStatus.UPLOADED)
                .build();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (processingStatus == null) {
            processingStatus = ProcessingStatus.UPLOADED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}