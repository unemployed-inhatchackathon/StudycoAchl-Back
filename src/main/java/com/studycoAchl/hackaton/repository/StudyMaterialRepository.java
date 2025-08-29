package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, UUID> {

    // 사용자별 학습자료 조회
    List<StudyMaterial> findByAppUsers_Uuid(UUID userUuid);

    // 과목별 학습자료 조회
    List<StudyMaterial> findBySubject_Uuid(UUID subjectUuid);

    // 사용자 + 과목별 학습자료 조회
    List<StudyMaterial> findByAppUsers_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);

    // 파일명으로 조회
    Optional<StudyMaterial> findByFileName(String fileName);

    // 처리 상태별 조회
    List<StudyMaterial> findByProcessingStatus(StudyMaterial.ProcessingStatus status);

    // 요약이 완료된 자료들
    @Query("SELECT sm FROM StudyMaterial sm WHERE sm.aiSummary IS NOT NULL AND sm.aiSummary != ''")
    List<StudyMaterial> findMaterialsWithSummary();

    // 요약이 없는 자료들 (백그라운드 처리용)
    @Query("SELECT sm FROM StudyMaterial sm WHERE sm.aiSummary IS NULL OR sm.aiSummary = ''")
    List<StudyMaterial> findMaterialsWithoutSummary();

    // 사용자별 요약된 자료 개수
    @Query("SELECT COUNT(sm) FROM StudyMaterial sm WHERE sm.appUsers.uuid = :userUuid AND sm.aiSummary IS NOT NULL")
    long countSummarizedMaterialsByUser(@Param("userUuid") UUID userUuid);

    // 과목별 자료 개수
    long countBySubject_Uuid(UUID subjectUuid);

    // 최신순 정렬
    List<StudyMaterial> findByAppUsers_UuidOrderByCreatedAtDesc(UUID userUuid);
}