package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    // ========== 기본 조회 메소드들 (Spring Data JPA 네이밍 컨벤션) ==========

    List<ChatSession> findByUserUuid(UUID userUuid);
    List<ChatSession> findBySubjectUuid(UUID subjectUuid);
    List<ChatSession> findByUserUuidAndSubjectUuid(UUID userUuid, UUID subjectUuid);

    // 제목 기반 검색
    @SuppressWarnings("unused")
    List<ChatSession> findByTitleContaining(String title);

    // 상태 기반 조회
    @SuppressWarnings("unused")
    List<ChatSession> findByStatus(ChatSession.SessionStatus status);
    List<ChatSession> findByUserUuidAndStatus(UUID userUuid, ChatSession.SessionStatus status);

    // 시간 기반 정렬
    List<ChatSession> findByUserUuidOrderByCreatedDataDesc(UUID userUuid);

    // ========== 키워드 및 문제 생성 관련 (@Query 사용) ==========

    // 키워드가 추출된 세션들
    @Query("SELECT cs FROM ChatSession cs WHERE cs.extractedKeywords IS NOT NULL AND cs.extractedKeywords != ''")
    @SuppressWarnings("unused")
    List<ChatSession> findSessionsWithKeywords();

    // 키워드로 세션 검색
    @Query("SELECT cs FROM ChatSession cs WHERE cs.extractedKeywords LIKE %:keyword%")
    @SuppressWarnings("unused")
    List<ChatSession> findByExtractedKeywordsContaining(@Param("keyword") String keyword);

    // 문제가 생성된 세션들
    @Query("SELECT cs FROM ChatSession cs WHERE cs.generatedProblemCount > 0")
    List<ChatSession> findSessionsWithProblems();

    // 사용자별 문제 생성된 세션들
    @Query("SELECT cs FROM ChatSession cs WHERE cs.userUuid = :userUuid AND cs.generatedProblemCount > 0")
    @SuppressWarnings("unused")
    List<ChatSession> findSessionsWithProblemsByUser(@Param("userUuid") UUID userUuid);

    @Query("SELECT cs FROM ChatSession cs " +
            "LEFT JOIN FETCH cs.subject " +
            "LEFT JOIN FETCH cs.user " +
            "LEFT JOIN FETCH cs.problems " +
            "WHERE cs.uuid = :uuid")
    Optional<ChatSession> findByIdWithAllRelations(@Param("uuid") UUID uuid);
}