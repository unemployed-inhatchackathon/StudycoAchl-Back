package com.studycoAchl.hackaton.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@EntityListeners(AuditingEntityListener.class)
public class ChatSession {
    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "chatTitle")
    private String chatTitle;

    @Column(name = "messages", columnDefinition = "JSON")
    private String messages;

    @CreatedDate
    @Column(name = "created_data")
    private LocalDateTime createdData;

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
    protected ChatSession() {}

    // 생성자
    public ChatSession(String uuid, String chatTitle, String userUuid, String subjectUuid) {
        this.uuid = uuid;
        this.chatTitle = chatTitle;
        this.userUuid = userUuid;
        this.subjectUuid = subjectUuid;
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getChatTitle() { return chatTitle; }
    public void setChatTitle(String chatTitle) { this.chatTitle = chatTitle; }
    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }
    public LocalDateTime getCreatedData() { return createdData; }
    public User getUser() { return user; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public Subject getSubject() { return subject; }
    public String getSubjectUuid() { return subjectUuid; }
    public void setSubjectUuid(String subjectUuid) { this.subjectUuid = subjectUuid; }
}