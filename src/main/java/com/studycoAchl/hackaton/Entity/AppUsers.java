package com.studycoAchl.hackaton.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "app_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", columnDefinition = "Binary(16)")
    private UUID uuid;

    // 기본 ID (필요시 사용)
    @Column(name = "id", unique = true)
    private Long id;

    @Column(name = "token", length = 500)
    private String token;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Subject와의 관계
    @OneToMany(mappedBy = "appUsers", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Subject> subjects = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // 카카오 로그인용 생성자
    public static AppUsers createKakaoUser(String email, String nickname, String profileImage) {
        return AppUsers.builder()
                .email(email)
                .username(nickname) // 카카오에서는 nickname을 username으로 사용
                .nickname(nickname)
                .profileImage(profileImage)
                .password("OAUTH_USER") // OAuth 사용자는 비밀번호가 없으므로 기본값
                .build();
    }

    // 일반 회원가입용 생성자
    public static AppUsers createGeneralUser(String email, String username, String password) {
        return AppUsers.builder()
                .email(email)
                .username(username)
                .nickname(username) // 기본적으로 username과 동일
                .password(password)
                .build();
    }
}