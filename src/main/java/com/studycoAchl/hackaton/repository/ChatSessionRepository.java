package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid")
    List<ChatSession> findByUserUuid(@Param("userUuid") String userUuid);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.subject.uuid = :subjectUuid")
    List<ChatSession> findBySubjectUuid(@Param("subjectUuid") String subjectUuid);

    // title 필드로 수정 (ChatSession 엔티티에서 title 필드 사용)
    List<ChatSession> findByTitleContaining(String title);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid AND cs.subject.uuid = :subjectUuid")
    List<ChatSession> findByUserUuidAndSubjectUuid(@Param("userUuid") String userUuid, @Param("subjectUuid") String subjectUuid);

    // createdAt 필드로 수정 (ChatSession 엔티티에서 createdAt 필드 사용)
    @Query("SELECT cs FROM ChatSession cs ORDER BY cs.createdAt DESC")
    List<ChatSession> findRecentSessions();

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid ORDER BY cs.createdAt DESC")
    List<ChatSession> findRecentSessionsByUser(@Param("userUuid") String userUuid);

    // 추가 유용한 메서드들
    @Query("SELECT cs FROM ChatSession cs WHERE cs.status = :status")
    List<ChatSession> findByStatus(@Param("status") ChatSession.SessionStatus status);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid AND cs.status = :status")
    List<ChatSession> findByUserUuidAndStatus(@Param("userUuid") String userUuid,
                                              @Param("status") ChatSession.SessionStatus status);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid AND cs.status = 'ACTIVE' ORDER BY cs.updatedAt DESC")
    List<ChatSession> findActiveSessionsByUser(@Param("userUuid") String userUuid);

    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.user.uuid = :userUuid")
    Long countByUserUuid(@Param("userUuid") String userUuid);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.generatedProblemCount > 0 ORDER BY cs.lastProblemGeneration DESC")
    List<ChatSession> findSessionsWithProblems();

    // 호환성을 위한 메서드들 (기존 코드에서 사용하는 메서드명 지원)
    @Query("SELECT cs FROM ChatSession cs WHERE cs.title LIKE %:title%")
    List<ChatSession> findByChatTitleContaining(@Param("title") String title);
}