package com.studycoAchl.hackaton.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.studycoAchl.hackaton.DTO.ChatMessage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions") // 복수형으로 변경
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "BINARY(16)")
    private UUID uuid;

    @Column(name = "title", length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "JSON")
    private List<ChatMessage> messages;

    @Column(name = "created_at") // 스네이크케이스로 변경
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "uuid")
    @JsonBackReference
    private Subject subject;

    // 생성자
    public ChatSession(User user, Subject subject, String title) {
        this();
        this.user = user;
        this.subject = subject;
        this.title = title;
    }

    // 편의 메소드들
    public void addMessage(String sender, String content) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }

        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                sender,
                content,
                LocalDateTime.now()
        );

        this.messages.add(message);
    }

    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    @PrePersist
    protected void onCreate() {
        // UUID 자동 생성
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}