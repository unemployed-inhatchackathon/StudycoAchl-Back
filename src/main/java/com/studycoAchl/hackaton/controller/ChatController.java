package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.dto.ChatMessage;
import com.studycoAchl.hackaton.dto.MessageRequest;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.service.ChatSessionService;
import com.studycoAchl.hackaton.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatSessionService chatSessionService;
    private final AiService aiService;
    // KeywordExtractionService 제거 - 직접 AiService 사용

    // === 교육적 내용 판단 헬퍼 메소드 ===
    private boolean isEducationalContent(String content) {
        if (content == null || content.trim().length() < 5) {
            return false;
        }

        // 교육 관련 키워드 체크
        String[] educationalKeywords = {
                "배우", "공부", "알고싶", "설명", "이해", "방법", "어떻게", "무엇", "왜",
                "함수", "방정식", "공식", "계산", "문제", "풀이", "해결", "개념", "정의",
                "수학", "과학", "영어", "문법", "단어", "이론", "원리", "법칙"
        };

        String lowerContent = content.toLowerCase();
        for (String keyword : educationalKeywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }

        // 질문 형태 체크
        return content.contains("?") || content.contains("？");
    }

    // === 키워드 추출 및 저장 헬퍼 메소드 ===
    private void extractAndSaveKeywords(UUID sessionUuid, String content, String subjectName) {
        try {
            // AiService를 직접 사용해서 키워드 추출
            String extractedKeywords = aiService.extractKeywords(content, subjectName);

            if (extractedKeywords != null && !extractedKeywords.trim().isEmpty()) {
                // 쉼표로 분할하고 정제
                String[] keywordArray = extractedKeywords.split(",");

                for (String keyword : keywordArray) {
                    String cleanKeyword = keyword.trim();
                    if (cleanKeyword.length() >= 2 && cleanKeyword.length() <= 20) {
                        chatSessionService.addExtractedKeyword(sessionUuid, cleanKeyword);
                    }
                }

                log.debug("키워드 추출 완료 - sessionUuid: {}, keywords: {}", sessionUuid, extractedKeywords);
            }
        } catch (Exception e) {
            log.warn("키워드 추출 중 오류 - sessionUuid: {}", sessionUuid, e);
        }
    }

    /**
     * 사용자별 채팅 세션 목록 조회
     */
    @GetMapping("/users/{userUuid}/sessions")
    public ResponseEntity<ApiResponse<List<ChatSession>>> getUserSessions(@PathVariable UUID userUuid) {
        try {
            List<ChatSession> sessions = chatSessionService.findByUser(userUuid);
            return ResponseEntity.ok(ApiResponse.success(sessions, "채팅 세션 목록을 조회했습니다."));
        } catch (Exception e) {
            log.error("사용자 세션 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("세션 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 채팅 세션 생성
     */
    @PostMapping(value = "/users/{userUuid}/subjects/{subjectUuid}/sessions", consumes = "text/plain")
    public ResponseEntity<ApiResponse<ChatSession>> createSession(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid,
            @RequestBody String title) {

        try {
            ChatSession createdSession = chatSessionService.createChatSession(userUuid, subjectUuid, title);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdSession, "채팅 세션이 생성되었습니다."));
        } catch (Exception e) {
            log.error("채팅 세션 생성 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("채팅 세션 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 채팅 세션 상세 조회
     */
    @GetMapping("/sessions/detail/{sessionUuid}")
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 추가
    public ResponseEntity<ApiResponse<ChatSession>> getSessionDetail(@PathVariable UUID sessionUuid) {
        try {
            ChatSession session = chatSessionService.findById(sessionUuid);
            return ResponseEntity.ok(ApiResponse.success(session, "채팅 세션을 조회했습니다."));
        } catch (Exception e) {
            log.error("세션 상세 조회 실패 - sessionUuid: {}", sessionUuid, e);
            return ResponseEntity.ok(ApiResponse.error("세션을 찾을 수 없습니다."));
        }
    }

    /**
     * 메시지 전송 및 AI 응답 생성 - 자동 키워드 추출 추가
     */
    @PostMapping("/users/{userUuid}/subjects/{subjectUuid}/sessions/{sessionUuid}/messages")
    @Transactional // 🎯 이것이 핵심! 트랜잭션 추가
    public ResponseEntity<ApiResponse<ChatSession>> addMessage(
            @PathVariable UUID sessionUuid,
            @RequestBody MessageRequest messageRequest) {

        try {
            ChatSession session = chatSessionService.findById(sessionUuid);

            // 1. 사용자 메시지 추가 (교육적 내용 판단)
            boolean isEducational = isEducationalContent(messageRequest.getContent());
            ChatMessage userMessage = new ChatMessage(
                    UUID.randomUUID().toString(),
                    messageRequest.getSender(),
                    messageRequest.getContent(),
                    LocalDateTime.now(),
                    isEducational
            );

            session.addMessage(messageRequest.getSender(), messageRequest.getContent());

            // 2. 사용자 메시지면 AI 응답 생성
            if ("USER".equalsIgnoreCase(messageRequest.getSender())) {
                try {
                    String subjectName = session.getSubject().getTitle();

                    // AI 응답 생성
                    String aiResponse = aiService.generateResponse(messageRequest.getContent(), subjectName);

                    // AI 메시지 추가 (AI 응답도 교육적 내용으로 간주)
                    session.addMessage("AI", aiResponse);

                    log.info("AI 응답 생성 완료 - sessionUuid: {}", sessionUuid);

                    // 🔥 3. 자동 키워드 추출 (사용자 메시지와 AI 응답 모두 분석)
                    if (isEducational) {
                        try {
                            // 사용자 메시지에서 키워드 추출
                            extractAndSaveKeywords(sessionUuid, messageRequest.getContent(), subjectName);

                            // AI 응답에서도 키워드 추출
                            extractAndSaveKeywords(sessionUuid, aiResponse, subjectName);

                            log.info("자동 키워드 추출 완료 - sessionUuid: {}", sessionUuid);
                        } catch (Exception keywordError) {
                            log.warn("키워드 추출 실패 (계속 진행) - sessionUuid: {}", sessionUuid, keywordError);
                            // 키워드 추출 실패해도 채팅은 계속 진행
                        }
                    }

                } catch (Exception e) {
                    log.error("AI 응답 생성 실패 - sessionUuid: {}", sessionUuid, e);
                    // AI 오류 시 에러 메시지 추가
                    session.addMessage("AI", "죄송합니다. 현재 AI 서비스에 문제가 있습니다.");
                }
            }

            ChatSession updatedSession = chatSessionService.save(session);
            return ResponseEntity.ok(ApiResponse.success(updatedSession, "메시지가 전송되었습니다."));

        } catch (Exception e) {
            log.error("메시지 전송 실패 - sessionUuid: {}", sessionUuid, e);
            return ResponseEntity.ok(ApiResponse.error("메시지 전송에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 세션 제목 수정
     */
    @PutMapping("/sessions/{sessionUuid}")
    @Transactional // 트랜잭션 추가
    public ResponseEntity<ApiResponse<ChatSession>> updateSessionTitle(
            @PathVariable UUID sessionUuid,
            @RequestBody Map<String, String> request) {

        try {
            ChatSession session = chatSessionService.findById(sessionUuid);
            session.setTitle(request.get("title"));

            ChatSession updatedSession = chatSessionService.save(session);
            return ResponseEntity.ok(ApiResponse.success(updatedSession, "세션 제목이 수정되었습니다."));

        } catch (Exception e) {
            log.error("세션 제목 수정 실패 - sessionUuid: {}", sessionUuid, e);
            return ResponseEntity.ok(ApiResponse.error("세션 제목 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 채팅 세션 삭제
     */
    @DeleteMapping("/sessions/{sessionUuid}")
    public ResponseEntity<ApiResponse<String>> deleteSession(@PathVariable UUID sessionUuid) {
        try {
            chatSessionService.deleteSession(sessionUuid);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "채팅 세션이 성공적으로 삭제되었습니다."));

        } catch (Exception e) {
            log.error("세션 삭제 실패 - sessionUuid: {}", sessionUuid, e);
            return ResponseEntity.ok(ApiResponse.error("세션 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 과목별 세션 조회
     */
    @GetMapping("/users/{userUuid}/subjects/{subjectUuid}/sessions")
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 추가
    public ResponseEntity<ApiResponse<List<ChatSession>>> getSessionsBySubject(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid) {

        try {
            List<ChatSession> sessions = chatSessionService.findByUserAndSubject(userUuid, subjectUuid);
            return ResponseEntity.ok(ApiResponse.success(sessions, "과목별 세션 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("과목별 세션 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목별 세션 조회에 실패했습니다."));
        }
    }

    /**
     * 연결 테스트
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> test() {
        Map<String, Object> testData = Map.of(
                "status", "연결 성공",
                "service", "채팅 API",
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(testData, "채팅 API 연결 성공!"));
    }
}