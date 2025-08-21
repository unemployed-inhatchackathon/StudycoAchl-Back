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

    // User와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    @JsonBackReference
    private User user;

    // ChatSession과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @JsonIgnore
    @Builder.Default
    private List<ChatSession> chatSessions = new ArrayList<>();

    // Exams와의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Exams> exams = new ArrayList<>();

    // Problem과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Problem> problems = new ArrayList<>();

    // 생성자 (편의 메소드)
    public Subject(User user, String title) {
        this.user = user;
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