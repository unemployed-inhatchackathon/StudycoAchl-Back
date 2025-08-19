package com.studycoAchl.hackaton.Service;

import com.studycoAchl.hackaton.Entity.ChatSession;
import com.studycoAchl.hackaton.Entity.Subject;
import com.studycoAchl.hackaton.Entity.User;
import com.studycoAchl.hackaton.Repository.ChatSessionRepository;
import com.studycoAchl.hackaton.Repository.SubjectRepository;
import com.studycoAchl.hackaton.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;  // 추가 필요
    private final SubjectRepository subjectRepository;  // 추가 필요

    public ChatSessionService(ChatSessionRepository chatSessionRepository,
                              UserRepository userRepository,
                              SubjectRepository subjectRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    public ChatSession createChatSession(UUID userUuid, UUID subjectUuid, String title) {
        validateTitle(title);

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Subject subject = subjectRepository.findById(subjectUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다."));

        ChatSession chatSession = ChatSession.builder()
                .user(user)
                .subject(subject)
                .title(title.trim())
                .messages(new ArrayList<>())
                .build();

        return chatSessionRepository.save(chatSession);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("제목은 100자를 초과할 수 없습니다.");
        }
    }
}
