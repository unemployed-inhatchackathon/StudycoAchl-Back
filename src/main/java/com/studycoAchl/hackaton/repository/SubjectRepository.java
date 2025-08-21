package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    // ========== 기본 조회 메소드들 (Spring Data JPA 네이밍 컨벤션) ==========

    List<Subject> findByUser_Uuid(UUID userUuid);

    // 제목 기반 검색
    List<Subject> findByTitleContaining(String title);
    Optional<Subject> findByUser_UuidAndTitle(UUID userUuid, String title);

    // 시간 기반 정렬
    List<Subject> findByUser_UuidOrderByCreatedAtDesc(UUID userUuid);

    // 존재 여부 확인
    boolean existsByUser_UuidAndTitle(UUID userUuid, String title);
}