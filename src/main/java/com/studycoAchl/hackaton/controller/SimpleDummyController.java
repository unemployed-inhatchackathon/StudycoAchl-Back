package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.entity.User;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import com.studycoAchl.hackaton.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simple-dummy")
@RequiredArgsConstructor
@Slf4j
public class SimpleDummyController {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ChatSessionRepository chatSessionRepository;

    @GetMapping("/create")
    @Transactional
    public Map<String, Object> createSimpleData() {
        try {
            log.info("간단한 테스트 데이터 생성 시작");

            // 1. User 생성 (UUID 자동 생성)
            User user = new User();
            user.setEmail("simple@test.com");
            user.setPassword("password");
            user.setNickname("간단테스트");
            // UUID를 설정하지 않고 JPA가 자동 생성하도록 함

            User savedUser = userRepository.saveAndFlush(user);
            log.info("사용자 생성 완료: {}", savedUser.getUuid());

            // 2. Subject 생성
            Subject subject = new Subject();
            subject.setTitle("테스트과목");
            subject.setUser(savedUser);

            Subject savedSubject = subjectRepository.saveAndFlush(subject);
            log.info("과목 생성 완료: {}", savedSubject.getUuid());

            // 3. ChatSession 생성
            ChatSession chatSession = new ChatSession();
            chatSession.setTitle("테스트채팅");
            chatSession.setUser(savedUser);
            chatSession.setSubject(savedSubject);
            chatSession.setStatus(ChatSession.SessionStatus.ACTIVE);

            ChatSession savedChatSession = chatSessionRepository.saveAndFlush(chatSession);
            log.info("채팅세션 생성 완료: {}", savedChatSession.getUuid());

            return Map.of(
                    "success", true,
                    "message", "간단한 테스트 데이터 생성 성공!",
                    "userUuid", savedUser.getUuid().toString(),
                    "subjectUuid", savedSubject.getUuid().toString(),
                    "chatSessionUuid", savedChatSession.getUuid().toString(),
                    "instruction", "위의 chatSessionUuid를 복사해서 API 테스트에 사용하세요!"
            );

        } catch (Exception e) {
            log.error("간단한 데이터 생성 실패", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            );
        }
    }

    @GetMapping("/clear")
    @Transactional
    public Map<String, Object> clearAll() {
        try {
            chatSessionRepository.deleteAll();
            subjectRepository.deleteAll();
            userRepository.deleteAll();

            return Map.of(
                    "success", true,
                    "message", "모든 데이터가 삭제되었습니다!"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    @GetMapping("/test-basic")
    public Map<String, Object> testBasic() {
        return Map.of(
                "success", true,
                "message", "기본 엔드포인트 작동 중",
                "timestamp", LocalDateTime.now()
        );
    }
}