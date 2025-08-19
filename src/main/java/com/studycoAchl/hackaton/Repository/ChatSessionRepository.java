package com.studycoAchl.hackaton.Repository;

import com.studycoAchl.hackaton.Entity.ChatSession;
import com.studycoAchl.hackaton.Entity.Subject;
import com.studycoAchl.hackaton.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUser_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);
    List<ChatSession> findByUser_Uuid(UUID userUuid);
    List<ChatSession> findBySubject_Uuid(UUID subjectUuid);
    List<ChatSession> findBySubject_UuidAndTitleContaining(UUID subjectUuid, String title);

}