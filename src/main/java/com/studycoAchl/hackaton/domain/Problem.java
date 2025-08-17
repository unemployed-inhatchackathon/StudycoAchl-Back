package com.studycoAchl.hackaton.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem")
@EntityListeners(AuditingEntityListener.class)
public class Problem {
    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "problems", columnDefinition = "JSON")
    private String problems;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_uuid", referencedColumnName = "UUID")
    private ChatSession chatSession;

    @Column(name = "chat_session_uuid", insertable = false, updatable = false)
    private String chatSessionUuid;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 기본 생성자
    protected Problem() {}

    // 생성자
    public Problem(String uuid, String problems, String userUuid, String subjectUuid, String chatSessionUuid) {
        this.uuid = uuid;
        this.problems = problems;
        this.userUuid = userUuid;
        this.subjectUuid = subjectUuid;
        this.chatSessionUuid = chatSessionUuid;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getProblems() { return problems; }
    public void setProblems(String problems) { this.problems = problems; }
    public User getUser() { return user; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public Subject getSubject() { return subject; }
    public String getSubjectUuid() { return subjectUuid; }
    public void setSubjectUuid(String subjectUuid) { this.subjectUuid = subjectUuid; }
    public ChatSession getChatSession() { return chatSession; }
    public String getChatSessionUuid() { return chatSessionUuid; }
    public void setChatSessionUuid(String chatSessionUuid) { this.chatSessionUuid = chatSessionUuid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}