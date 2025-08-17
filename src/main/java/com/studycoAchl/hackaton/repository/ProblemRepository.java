package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, String> {

    List<Problem> findByUserUuidOrderByCreatedAtDesc(String userUuid);

    List<Problem> findBySubjectUuidOrderByCreatedAtDesc(String subjectUuid);

    List<Problem> findByChatSessionUuidOrderByCreatedAtDesc(String chatSessionUuid);

    List<Problem> findByUserUuidAndSubjectUuidOrderByCreatedAtDesc(String userUuid, String subjectUuid);

    // 특정 채팅 세션에서 생성된 문제들
    List<Problem> findByUserUuidAndChatSessionUuidOrderByCreatedAtDesc(String userUuid, String chatSessionUuid);
}