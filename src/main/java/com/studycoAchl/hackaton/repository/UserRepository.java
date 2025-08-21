package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ========== 기본 조회 메소드들 (Spring Data JPA 네이밍 컨벤션) ==========

    // 로그인/회원가입용
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 닉네임 중복 체크용
    Optional<User> findByNickname(String nickname);
    boolean existsByNickname(String nickname);

    // 인증용
    Optional<User> findByToken(String token);
}