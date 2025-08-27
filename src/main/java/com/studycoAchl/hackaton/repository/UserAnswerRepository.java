package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, UUID> {

    // 기본 조회
    List<UserAnswer> findByQuizResult_Uuid(UUID quizResultUuid);

    // 정답/오답별 조회
    List<UserAnswer> findByQuizResult_UuidAndIsCorrect(UUID quizResultUuid, Boolean isCorrect);

    // 틀린 답안들만 조회 (오답노트용)
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.quizResult.appUsers.uuid = :userUuid AND ua.isCorrect = false")
    List<UserAnswer> findWrongAnswersByUser(@Param("userUuid") UUID userUuid);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.quizResult.appUsers.uuid = :userUuid AND ua.quizResult.subject.uuid = :subjectUuid AND ua.isCorrect = false")
    List<UserAnswer> findWrongAnswersByUserAndSubject(@Param("userUuid") UUID userUuid, @Param("subjectUuid") UUID subjectUuid);
}