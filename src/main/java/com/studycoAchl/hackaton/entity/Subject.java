package com.studycoAchl.hackaton.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subject")
@EntityListeners(AuditingEntityListener.class)
public class Subject {
    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    private User user;

    @Column(name = "user_uuid", insertable = false, updatable = false)
    private String userUuid;

    @CreatedDate
    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    // 기본 생성자
    protected Subject() {}

    // 생성자
    public Subject(String uuid, String title, String userUuid) {
        this.uuid = uuid;
        this.title = title;
        this.userUuid = userUuid;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public User getUser() { return user; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}