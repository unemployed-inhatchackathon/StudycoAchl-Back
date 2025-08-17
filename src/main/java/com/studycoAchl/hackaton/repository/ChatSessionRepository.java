package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByUserUuidOrderByCreatedDataDesc(String userUuid);

    List<ChatSession> findBySubjectUuidOrderByCreatedDataDesc(String subjectUuid);

    List<ChatSession> findByUserUuidAndSubjectUuidOrderByCreatedDataDesc(String userUuid, String subjectUuid);

    List<ChatSession> findByChatTitleContainingAndUserUuid(String chatTitle, String userUuid);
}