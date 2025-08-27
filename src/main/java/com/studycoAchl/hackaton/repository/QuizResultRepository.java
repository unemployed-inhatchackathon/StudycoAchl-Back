package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, UUID> {

    // 기본 조회
    List<QuizResult> findByUser_Uuid(UUID userUuid);
    List<QuizResult> findBySubject_Uuid(UUID subjectUuid);
    Optional<QuizResult> findByProblem_Uuid(UUID problemUuid);

    // 상태별 조회
    List<QuizResult> findByUser_UuidAndStatus(UUID userUuid, QuizResult.ResultStatus status);

    // 최신순 조회
    List<QuizResult> findByUser_UuidOrderByCompletedAtDesc(UUID userUuid);

    // 통계 쿼리
    @Query("SELECT AVG(qr.score) FROM QuizResult qr WHERE qr.user.uuid = :userUuid AND qr.status = 'COMPLETED'")
    Double getAverageScoreByUser(@Param("userUuid") UUID userUuid);

    @Query("SELECT COUNT(qr) FROM QuizResult qr WHERE qr.user.uuid = :userUuid AND qr.status = 'COMPLETED'")
    Long countCompletedQuizzesByUser(@Param("userUuid") UUID userUuid);
}