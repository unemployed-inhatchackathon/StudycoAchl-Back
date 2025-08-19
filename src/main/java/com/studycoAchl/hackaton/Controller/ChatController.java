package com.studycoAchl.hackaton.Controller;

import com.studycoAchl.hackaton.DTO.ChatMessage;
import com.studycoAchl.hackaton.DTO.CreateSession;
import com.studycoAchl.hackaton.DTO.MessageRequest;
import com.studycoAchl.hackaton.Entity.ChatSession;
import com.studycoAchl.hackaton.Repository.ChatSessionRepository;
import com.studycoAchl.hackaton.Service.ChatSessionService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import com.studycoAchl.hackaton.Service.aiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatSessionRepository chatSessionRepository;

    private final ChatSessionService chatSessionService;

    private final aiService aiService;

    public ChatController(ChatSessionRepository chatSessionRepository,
                          ChatSessionService chatSessionService,
                          aiService aiService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatSessionService = chatSessionService;
        this.aiService = aiService;
    }

    // 채팅방 목록 조회
    @GetMapping("/users/{userUuid}/sessions")
    public ResponseEntity<List<ChatSession>> getUserSessions(@PathVariable @Parameter UUID userUuid) {
        List<ChatSession> sessions = chatSessionRepository.findByUser_Uuid(userUuid);
        return ResponseEntity.ok(sessions);
    }

    // 채팅방 생성
    @PostMapping(value = "/users/{userUuid}/subjects/{subjectUuid}/sessions", consumes = "text/plain")
    public ResponseEntity<ChatSession> createSession(
            @PathVariable
            @Parameter
            UUID userUuid,
            @PathVariable
            @Parameter
            UUID subjectUuid,
            @RequestBody String title) {

        ChatSession createdSession = chatSessionService.createChatSession(userUuid, subjectUuid, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
    }

    private String getCurrentUserUuid(HttpServletRequest request) {
        return "user-sample-uuid-123456789012"; // 임시
    }

    //채팅방 조회
    @GetMapping("/sessions/detail/{sessionUuid}")
    public ResponseEntity<ChatSession> getSessionDetail(@PathVariable @Parameter UUID sessionUuid) {
        Optional<ChatSession> session = chatSessionRepository.findById(sessionUuid);

        if (session.isPresent()) {
            return ResponseEntity.ok(session.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{userUuid}/subjects/{subjectUuid}/sessions/{sessionUuid}/messages")
    public ResponseEntity<ChatSession> addMessage(
            @PathVariable UUID sessionUuid,
            @RequestBody MessageRequest messageRequest) {

        Optional<ChatSession> optionalSession = chatSessionRepository.findById(sessionUuid);

        if (optionalSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatSession session = optionalSession.get();

        // 1. 사용자 메시지 생성 및 저장
        ChatMessage userMessage = new ChatMessage(
                UUID.randomUUID().toString(),
                messageRequest.getSender(),
                messageRequest.getContent(),
                messageRequest.getImageUrl(),
                LocalDateTime.now()
        );

        session.getMessages().add(userMessage);

        // 2. 사용자가 보낸 메시지면 AI 응답 생성
        if ("USER".equals(messageRequest.getSender())) {
            try {
                String subjectName = session.getSubject().getName();

                // AI 응답 생성
                String aiResponse = aiService.generateResponse(messageRequest.getContent(), subjectName);

                // AI 메시지 생성 및 추가
                ChatMessage aiMessage = new ChatMessage(
                        UUID.randomUUID().toString(),
                        "AI",
                        aiResponse,
                        null,
                        LocalDateTime.now()
                );

                session.getMessages().add(aiMessage);
            } catch (Exception e) {
                // AI 오류 시 에러 메시지 추가
                ChatMessage errorMessage = new ChatMessage(
                        UUID.randomUUID().toString(),
                        "AI",
                        "죄송합니다. 현재 AI 서비스에 문제가 있습니다.",
                        null,
                        LocalDateTime.now()
                );
                session.getMessages().add(errorMessage);
            }
        }

        session.setCreatedAt(LocalDateTime.now());
        ChatSession updatedSession = chatSessionRepository.save(session);
        return ResponseEntity.ok(updatedSession);
    }

    //제목 수정
    @PutMapping("/sessions/{sessionUuid}")
    public ResponseEntity<ChatSession> updateSessionTitle(
            @PathVariable @Parameter UUID sessionUuid,
            @RequestBody Map<UUID, String> request) {

        Optional<ChatSession> optionalSession = chatSessionRepository.findById(sessionUuid);

        if (optionalSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatSession session = optionalSession.get();
        session.setTitle(request.get("title"));

        ChatSession updatedSession = chatSessionRepository.save(session);
        return ResponseEntity.ok(updatedSession);
    }
    //채팅방 삭제
    @DeleteMapping("/sessions/{sessionUuid}")
    public ResponseEntity<String> deleteSession(@PathVariable @Parameter UUID sessionUuid) {
        Optional<ChatSession> optionalSession = chatSessionRepository.findById(sessionUuid);

        if (optionalSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        chatSessionRepository.deleteById(sessionUuid);
        return ResponseEntity.ok("채팅방이 성공적으로 삭제되었습니다.");
    }

    //채팅방 조회
    @GetMapping("/users/{userUuid}/subjects/{subjectUuid}/sessions")
    public ResponseEntity<List<ChatSession>> getSessionsBySubject(
            @PathVariable @Parameter UUID userUuid,
            @PathVariable @Parameter UUID subjectUuid) {

        List<ChatSession> sessions = chatSessionRepository.findByUser_UuidAndSubject_Uuid(userUuid, subjectUuid);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("채팅 API 연결 성공! 현재 시간: " + LocalDateTime.now());
    }
}
