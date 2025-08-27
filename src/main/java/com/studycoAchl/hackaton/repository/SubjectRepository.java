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
    List<Subject> findByUserUuid(UUID userUuid);

    // 제목 검색
    List<Subject> findByTitleContaining(String title);


    // 사용자 UUID + 제목으로 단일 과목 조회
    Optional<Subject> findByUserUuidAndTitle(UUID userUuid, String title);

    // 사용자 UUID 기준 + 생성일 역순 정렬
    List<Subject> findByUserUuidOrderByCreatedAtDesc(UUID userUuid);

    // 사용자 UUID + 제목 존재 여부 확인
    boolean existsByUserUuidAndTitle(UUID userUuid, String title);
}