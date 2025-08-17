package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByToken(String token);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}