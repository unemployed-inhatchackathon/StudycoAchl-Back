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

    List<ChatSession> findByChatTitleContaining(String title);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid AND cs.subject.uuid = :subjectUuid")
    List<ChatSession> findByUserUuidAndSubjectUuid(@Param("userUuid") String userUuid, @Param("subjectUuid") String subjectUuid);

    @Query("SELECT cs FROM ChatSession cs ORDER BY cs.createdData DESC")
    List<ChatSession> findRecentSessions();

    @Query("SELECT cs FROM ChatSession cs WHERE cs.user.uuid = :userUuid ORDER BY cs.createdData DESC")
    List<ChatSession> findRecentSessionsByUser(@Param("userUuid") String userUuid);
}