package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    // ========== 기본 조회 메소드들 (Spring Data JPA 네이밍 컨벤션) ==========

    // 채팅 세션별 문제 조회
    Optional<Problem> findByChatSession_Uuid(UUID chatSessionUuid);
    List<Problem> findAllByChatSession_Uuid(UUID chatSessionUuid);

    // 사용자별 문제 조회
    List<Problem> findByUser_Uuid(UUID userUuid);

    // 과목별 문제 조회
    List<Problem> findBySubject_Uuid(UUID subjectUuid);

    // 사용자 + 과목별 문제 조회
    List<Problem> findByUser_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);

    // 시간 기반 정렬 (최신순)
    List<Problem> findByUser_UuidOrderByCreatedDataDesc(UUID userUuid);

    // 채팅 세션이 연결된 문제들 (채팅에서 생성된 문제)
    List<Problem> findByChatSessionIsNotNull();

    // 직접 생성된 문제들 (채팅과 무관하게 생성)
    List<Problem> findByChatSessionIsNull();
}