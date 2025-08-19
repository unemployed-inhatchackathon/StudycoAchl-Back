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

    /**
     * 채팅 세션 UUID로 문제 조회
     * 실제 Problem 엔티티의 필드명에 맞게 수정 필요
     */
    @Query("SELECT p FROM Problem p WHERE p.chatSessionUuid = :chatSessionUuid")
    Optional<Problem> findByChatSessionUuid(@Param("chatSessionUuid") String chatSessionUuid);

    // 혹시 필드명이 다른 경우를 위한 대안 메서드들
    @Query(value = "SELECT * FROM problems WHERE chat_session_uuid = :chatSessionUuid", nativeQuery = true)
    Optional<Problem> findByChatSessionUuidNative(@Param("chatSessionUuid") String chatSessionUuid);

    /**
     * 사용자 UUID로 문제 조회
     */
    @Query("SELECT p FROM Problem p WHERE p.userUuid = :userUuid")
    List<Problem> findByUserUuid(@Param("userUuid") String userUuid);

    /**
     * 과목 UUID로 문제 조회
     */
    @Query("SELECT p FROM Problem p WHERE p.subjectUuid = :subjectUuid")
    List<Problem> findBySubjectUuid(@Param("subjectUuid") String subjectUuid);

    /**
     * 사용자와 과목으로 문제 조회
     */
    @Query("SELECT p FROM Problem p WHERE p.userUuid = :userUuid AND p.subjectUuid = :subjectUuid")
    List<Problem> findByUserUuidAndSubjectUuid(@Param("userUuid") String userUuid,
                                               @Param("subjectUuid") String subjectUuid);

    /**
     * 최근 생성된 문제들 조회
     */
    @Query("SELECT p FROM Problem p WHERE p.userUuid = :userUuid ORDER BY p.createdData DESC")
    List<Problem> findByUserUuidOrderByCreatedDataDesc(@Param("userUuid") String userUuid);

    /**
     * 특정 개수만큼 최근 문제 조회
     */
    @Query(value = "SELECT p FROM Problem p WHERE p.userUuid = :userUuid ORDER BY p.createdData DESC LIMIT :limit", nativeQuery = false)
    List<Problem> findTopByUserUuidOrderByCreatedDataDesc(@Param("userUuid") String userUuid, @Param("limit") int limit);

    /**
     * 채팅 세션이 연결된 문제들 조회
     */
    @Query("SELECT p FROM Problem p WHERE p.chatSessionUuid IS NOT NULL")
    List<Problem> findAllWithChatSession();

    /**
     * 직접 생성된 문제들 조회 (채팅 세션과 연결되지 않은)
     */
    @Query("SELECT p FROM Problem p WHERE p.chatSessionUuid IS NULL")
    List<Problem> findAllDirectlyGenerated();
}

// 만약 실제 Problem 엔티티의 필드명이 다르다면 아래와 같이 수정하세요:

/*
 * 예시: 만약 Problem 엔티티에서 필드명이 다르다면
 *
 * @Entity
 * public class Problem {
 *     private String chatSession; // chatSessionUuid가 아니라 chatSession이라면
 *     private String user;        // userUuid가 아니라 user라면
 *     private String subject;     // subjectUuid가 아니라 subject라면
 * }
 *
 * 이 경우 메서드명을 다음과 같이 변경:
 * Optional<Problem> findByChatSession(String chatSession);
 * List<Problem> findByUser(String user);
 * List<Problem> findBySubject(String subject);
 *
 * 또는 @Query를 사용하여 정확한 필드명 지정:
 * @Query("SELECT p FROM Problem p WHERE p.chatSession = :chatSessionId")
 * Optional<Problem> findByChatSessionUuid(@Param("chatSessionId") String chatSessionId);
 */