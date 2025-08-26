package com.studycoAchl.hackaton.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "subject")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "UUID", columnDefinition = "Binary(16)")
    private UUID uuid;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @JoinColumn(name = "user_uuid", columnDefinition = "BINARY(16)", foreignKey = @ForeignKey(name = "FK_subject_user"))
    private UUID userUuid;

    // Exams와의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Exams> exams = new ArrayList<>();

    //ChatSession과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatSession> chatSessions = new ArrayList<>();

    // Problem과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Problem> problems = new ArrayList<>();

    // 생성자 (편의 메소드)
    public Subject(UUID user_uuid, String title) {
        this.userUuid = user_uuid;
        this.title = title;
        this.chatSessions = new ArrayList<>();
        this.exams = new ArrayList<>();
        this.problems = new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}