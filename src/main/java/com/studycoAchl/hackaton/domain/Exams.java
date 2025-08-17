package com.studycoAchl.hackaton.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@EntityListeners(AuditingEntityListener.class)
public class Exams {
    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "title")
    private String title;

    @Column(name = "pro_su")
    private Integer proSu; // 문제 수

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

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 기본 생성자
    protected Exams() {}

    // 생성자
    public Exams(String uuid, String title, Integer proSu, String userUuid, String subjectUuid) {
        this.uuid = uuid;
        this.title = title;
        this.proSu = proSu;
        this.userUuid = userUuid;
        this.subjectUuid = subjectUuid;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getProSu() { return proSu; }
    public void setProSu(Integer proSu) { this.proSu = proSu; }
    public User getUser() { return user; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public Subject getSubject() { return subject; }
    public String getSubjectUuid() { return subjectUuid; }
    public void setSubjectUuid(String subjectUuid) { this.subjectUuid = subjectUuid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}