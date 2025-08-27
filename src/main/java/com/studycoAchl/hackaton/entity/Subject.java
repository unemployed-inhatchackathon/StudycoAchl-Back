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

    // AppUsers와의 관계 매핑 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    @JsonBackReference
    private AppUsers appUsers;

    // 기존 userUuid 필드는 유지 (호환성을 위해)
    @Column(name = "user_uuid", columnDefinition = "BINARY(16)", nullable = false, insertable = false, updatable = false)
    private UUID userUuid;

    // Exams와의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Exams> exams = new ArrayList<>();

    // ChatSession과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ChatSession> chatSessions = new ArrayList<>();

    // Problem과의 관계
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Problem> problems = new ArrayList<>();

    // 생성자 (편의 메소드) - AppUsers 객체 사용하도록 수정
    public Subject(AppUsers appUsers, String title) {
        this.appUsers = appUsers;
        this.title = title;
        this.chatSessions = new ArrayList<>();
        this.exams = new ArrayList<>();
        this.problems = new ArrayList<>();
    }

    // 기존 UUID 생성자도 유지 (호환성)
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
        // appUsers가 설정되어 있다면 userUuid 자동 설정
        if (appUsers != null && userUuid == null) {
            userUuid = appUsers.getUuid();
        }
    }
}