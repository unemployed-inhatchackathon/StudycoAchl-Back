package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, String> {

    // 채팅 세션으로 문제 조회
    @Query("SELECT p FROM Problem p WHERE p.chatSession.uuid = :chatSessionUuid")
    Optional<Problem> findByChatSessionUuid(@Param("chatSessionUuid") String chatSessionUuid);

    // 채팅 세션의 모든 문제들 조회
    @Query("SELECT p FROM Problem p WHERE p.chatSession.uuid = :chatSessionUuid")
    List<Problem> findAllByChatSessionUuid(@Param("chatSessionUuid") String chatSessionUuid);

    // 사용자별 문제 조회
    @Query("SELECT p FROM Problem p WHERE p.user.uuid = :userUuid")
    List<Problem> findByUserUuid(@Param("userUuid") String userUuid);

    // 과목별 문제 조회
    @Query("SELECT p FROM Problem p WHERE p.subject.uuid = :subjectUuid")
    List<Problem> findBySubjectUuid(@Param("subjectUuid") String subjectUuid);

    // 사용자와 과목으로 문제 조회
    @Query("SELECT p FROM Problem p WHERE p.user.uuid = :userUuid AND p.subject.uuid = :subjectUuid")
    List<Problem> findByUserUuidAndSubjectUuid(@Param("userUuid") String userUuid, @Param("subjectUuid") String subjectUuid);

    // 최근 생성된 문제들
    @Query("SELECT p FROM Problem p ORDER BY p.createdData DESC")
    List<Problem> findRecentProblems();

    // 특정 사용자의 최근 문제들
    @Query("SELECT p FROM Problem p WHERE p.user.uuid = :userUuid ORDER BY p.createdData DESC")
    List<Problem> findRecentProblemsByUser(@Param("userUuid") String userUuid);

    // 채팅 세션별 최근 문제들
    @Query("SELECT p FROM Problem p WHERE p.chatSession.uuid = :chatSessionUuid ORDER BY p.createdData DESC")
    List<Problem> findByChatSessionUuidOrderByCreatedDataDesc(@Param("chatSessionUuid") String chatSessionUuid);
}