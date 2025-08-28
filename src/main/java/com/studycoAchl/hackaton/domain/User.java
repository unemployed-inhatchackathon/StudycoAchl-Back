package com.studycoAchl.hackaton.domain;

public class User {
    private Long id;          // 필요하면 사용, 아니면 무시
    private String username;  // 지금 AuthService에서 쓰는 필드

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
}
