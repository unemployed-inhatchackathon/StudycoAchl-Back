package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.entity.AppUsers;
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
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.Map;

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

            // 기존 데이터 확인 후 있으면 삭제
            userRepository.deleteAll();
            subjectRepository.deleteAll();
            chatSessionRepository.deleteAll();

            // 1. User 생성
            AppUsers appUsers = AppUsers.builder()
                    .email("simple@test.com")
                    .password("password")
                    .nickname("간단테스트")
                    .createdAt(LocalDateTime.now())
                    .build();

            AppUsers savedAppUsers = userRepository.save(appUsers);
            log.info("사용자 생성 완료: {}", savedAppUsers.getUuid());

            // 2. Subject 생성
            Subject subject = Subject.builder()
                    .title("테스트과목")
                    .appUsers(savedAppUsers)
                    .createdAt(LocalDateTime.now())
                    .build();

            Subject savedSubject = subjectRepository.save(subject);
            log.info("과목 생성 완료: {}", savedSubject.getUuid());

            // 3. ChatSession 생성
            ChatSession chatSession = ChatSession.builder()
                    .title("테스트채팅")
                    .appUsers(savedAppUsers)
                    .subject(savedSubject)
                    .status(ChatSession.SessionStatus.ACTIVE)
                    .createdData(LocalDateTime.now())
                    .messages(new ArrayList<>())
                    .build();

            ChatSession savedChatSession = chatSessionRepository.save(chatSession);
            log.info("채팅세션 생성 완료: {}", savedChatSession.getUuid());

            return Map.of(
                    "success", true,
                    "message", "간단한 테스트 데이터 생성 성공!",
                    "userUuid", savedAppUsers.getUuid().toString(),
                    "subjectUuid", savedSubject.getUuid().toString(),
                    "chatSessionUuid", savedChatSession.getUuid().toString()
            );

        } catch (Exception e) {
            log.error("간단한 데이터 생성 실패", e);
            throw new RuntimeException("데이터 생성 실패: " + e.getMessage());
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