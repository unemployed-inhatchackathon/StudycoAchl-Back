package com.studycoAchl.hackaton.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "record")
@EntityListeners(AuditingEntityListener.class)
public class Record {
    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "fileSize")
    private BigInteger fileSize;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "ai_text", columnDefinition = "TEXT")
    private String aiText;

    @Column(name = "is_favorite")
    private Boolean isFavorite;

    @Column(name = "exp_at")
    private LocalDateTime expAt; // 만료일

    @Column(name = "is_expired")
    private LocalDateTime isExpired; // 만료 여부

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    private User user;

    @Column(name = "user_uuid", insertable = false, updatable = false)
    private String userUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "UUID")
    private Subject subject;

    @Column(name = "subject_uuid", insertable = false, updatable = false)
    private String subjectUuid;

    // 기본 생성자
    protected Record() {}

    // 생성자
    public Record(String uuid, String title, String filePath, String userUuid, String subjectUuid) {
        this.uuid = uuid;
        this.title = title;
        this.filePath = filePath;
        this.userUuid = userUuid;
        this.subjectUuid = subjectUuid;
        this.isFavorite = false;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public BigInteger getFileSize() { return fileSize; }
    public void setFileSize(BigInteger fileSize) { this.fileSize = fileSize; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getAiText() { return aiText; }
    public void setAiText(String aiText) { this.aiText = aiText; }
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    public LocalDateTime getExpAt() { return expAt; }
    public void setExpAt(LocalDateTime expAt) { this.expAt = expAt; }
    public LocalDateTime getIsExpired() { return isExpired; }
    public void setIsExpired(LocalDateTime isExpired) { this.isExpired = isExpired; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public Subject getSubject() { return subject; }
    public String getSubjectUuid() { return subjectUuid; }
    public void setSubjectUuid(String subjectUuid) { this.subjectUuid = subjectUuid; }
}