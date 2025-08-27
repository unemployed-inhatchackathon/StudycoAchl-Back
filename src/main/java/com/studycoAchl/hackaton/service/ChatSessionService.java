package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.dto.ChatSessionResponseDto;
import com.studycoAchl.hackaton.entity.AppUsers;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.repository.ChatSessionRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.studycoAchl.hackaton.dto.ChatMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    // ========== 채팅 세션 생성 및 관리 ==========

    public ChatSession createChatSession(UUID userUuid, UUID subjectUuid, String title) {
        validateTitle(title);

        AppUsers appUsers = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Subject subject = subjectRepository.findById(subjectUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다."));

        ChatSession chatSession = ChatSession.builder()
                .appUsers(appUsers)
                .subject(subject)
                .title(title.trim())
                .messages(new ArrayList<>())
                .status(ChatSession.SessionStatus.ACTIVE)
                .build();

        return chatSessionRepository.save(chatSession);
    }

    public ChatSessionResponseDto toResponseDto(ChatSession chatSession) {
        return ChatSessionResponseDto.builder()
                .uuid(chatSession.getUuid())
                .title(chatSession.getTitle())
                .createdData(chatSession.getCreatedData())
                .updatedAt(chatSession.getUpdatedAt())
                .extractedKeywordsList(chatSession.getExtractedKeywordsList())
                .generatedProblemCount(chatSession.getGeneratedProblemCount())
                .status(chatSession.getStatus().toString())
                .userUuid(chatSession.getAppUsers() != null ? chatSession.getAppUsers().getUuid() : null)
                .subjectUuid(chatSession.getSubject() != null ? chatSession.getSubject().getUuid() : null)
                .subjectTitle(chatSession.getSubject() != null ? chatSession.getSubject().getTitle() : null)
                .messageCount(chatSession.getMessageCount())
                .build();
    }

    // ========== 메시지 관리 ==========

    public ChatSession addMessage(UUID sessionUuid, String sender, String content) {
        ChatSession session = findById(sessionUuid);
        session.addMessage(sender, content);
        return chatSessionRepository.save(session);
    }

    public ChatMessage addEducationalMessage(UUID sessionUuid, String sender, String content, boolean hasEducationalContent) {
        ChatSession session = findById(sessionUuid);
        ChatMessage message = session.addEducationalMessage(sender, content, hasEducationalContent);
        chatSessionRepository.save(session);
        return message;
    }

    // ========== 조회 메소드들 ==========

    public ChatSession findById(UUID sessionUuid) {
        return chatSessionRepository.findByIdWithAllRelations(sessionUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅 세션입니다."));
    }

    public List<ChatSession> findByUser(UUID userUuid) {
        return chatSessionRepository.findByAppUsers_Uuid(userUuid);  // 원래대로 복구
    }

    public List<ChatSession> findBySubject(UUID subjectUuid) {
        return chatSessionRepository.findBySubject_Uuid(subjectUuid);  // 원래대로 복구
    }

    public List<ChatSession> findByUserAndSubject(UUID userUuid, UUID subjectUuid) {
        return chatSessionRepository.findByAppUsers_UuidAndSubject_Uuid(userUuid, subjectUuid);  // 원래대로 복구
    }

    public List<ChatSession> findActiveSessionsByUser(UUID userUuid) {
        return chatSessionRepository.findByAppUsers_UuidAndStatus(userUuid, ChatSession.SessionStatus.ACTIVE);  // 원래대로 복구
    }

    // ========== 키워드 관련 기능 (통합 기능) ==========

    public List<String> getMessageContents(UUID sessionUuid) {
        ChatSession session = findById(sessionUuid);
        return session.getMessages().stream()
                .map(ChatMessage::getContent)
                .toList();
    }

    public void addExtractedKeyword(UUID sessionUuid, String keyword) {
        ChatSession session = findById(sessionUuid);
        session.addExtractedKeyword(keyword);
        chatSessionRepository.save(session);
    }

    public List<String> getExtractedKeywords(UUID sessionUuid) {
        ChatSession session = findById(sessionUuid);
        return session.getExtractedKeywordsList();
    }

    public boolean canGenerateProblems(UUID sessionUuid) {
        ChatSession session = findById(sessionUuid);
        return session.canGenerateProblems();
    }

    // ========== 세션 상태 관리 ==========

    public ChatSession updateSessionStatus(UUID sessionUuid, ChatSession.SessionStatus status) {
        ChatSession session = findById(sessionUuid);
        session.setStatus(status);
        return chatSessionRepository.save(session);
    }

    public ChatSession completeSession(UUID sessionUuid) {
        return updateSessionStatus(sessionUuid, ChatSession.SessionStatus.COMPLETED);
    }

    public ChatSession pauseSession(UUID sessionUuid) {
        return updateSessionStatus(sessionUuid, ChatSession.SessionStatus.PAUSED);
    }

    // ========== 문제 생성 연결 (통합 기능) ==========

    public void incrementProblemCount(UUID sessionUuid) {
        ChatSession session = findById(sessionUuid);
        session.incrementProblemCount();
        chatSessionRepository.save(session);
    }

    public List<ChatSession> findSessionsWithProblems() {
        return chatSessionRepository.findSessionsWithProblems();
    }

    // ========== 저장 및 유틸리티 ==========

    public ChatSession save(ChatSession chatSession) {
        return chatSessionRepository.save(chatSession);
    }

    public void deleteSession(UUID sessionUuid) {
        ChatSession session = findById(sessionUuid);
        chatSessionRepository.delete(session);
    }

    // ========== 검증 메소드 ==========

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("제목은 100자를 초과할 수 없습니다.");
        }
    }
}