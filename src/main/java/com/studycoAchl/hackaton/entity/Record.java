package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "UUID", columnDefinition = "Binary(16)")
    private UUID uuid;

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
    private LocalDateTime expAt;

    @Column(name = "is_expired")
    private LocalDateTime isExpired;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    private User user;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}