package com.studycoAchl.hackaton.Repository;

import com.studycoAchl.hackaton.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // 기본 CRUD 외에 필요한 메소드들
    boolean existsByEmail(String email);
    User findByEmail(String email);
}
