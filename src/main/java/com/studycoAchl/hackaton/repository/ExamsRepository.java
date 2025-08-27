package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Exams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamsRepository extends JpaRepository<Exams, UUID> {

    // ========== 기본 조회 메소드들 - appUsers로 수정 ==========

    // 사용자별 시험 조회
    List<Exams> findByAppUsers_Uuid(UUID userUuid);

    // 과목별 시험 조회
    List<Exams> findBySubject_Uuid(UUID subjectUuid);

    // 사용자 + 과목별 시험 조회
    List<Exams> findByAppUsers_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);

    // 제목으로 시험 검색
    List<Exams> findByTitleContaining(String title);
}