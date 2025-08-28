package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.dto.ChatMessage;
import com.studycoAchl.hackaton.dto.ChatSessionResponseDto;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatSessionService chatSessionService;
    private final AiService aiService;

    /**
     * 교육적 내용 판단 헬퍼 메소드
     */
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

    /**
     * 키워드 추출 및 저장 헬퍼 메소드 - 안전한 처리
     */
    private void extractAndSaveKeywords(UUID sessionUuid, String content, String subjectName) {
        try {
            if (content == null || content.trim().isEmpty()) {
                return;
            }

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
            log.warn("키워드 추출 중 오류 (계속 진행) - sessionUuid: {}", sessionUuid, e);
        }
    }

    /**
     * 사용자별 채팅 세션 목록 조회
     */
    @GetMapping("/users/{userUuid}/sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ChatSessionResponseDto>>> getUserSessions(@PathVariable UUID userUuid) {
        try {
            List<ChatSession> sessions = chatSessionService.findByUser(userUuid);
            List<ChatSessionResponseDto> response = sessions.stream()
                    .map(chatSessionService::toResponseDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(response, "채팅 세션 목록을 조회했습니다."));
        } catch (Exception e) {
            log.error("사용자 세션 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("세션 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 채팅 세션 생성
     */
    @PostMapping(value = "/users/{userUuid}/subjects/{subjectUuid}/sessions", consumes = "text/plain")
    public ResponseEntity<ApiResponse<ChatSessionResponseDto>> createSession(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid,
            @RequestBody String title) {

        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("세션 제목은 필수입니다."));
            }

            ChatSession createdSession = chatSessionService.createChatSession(userUuid, subjectUuid, title.trim());
            ChatSessionResponseDto response = chatSessionService.toResponseDto(createdSession);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "채팅 세션이 생성되었습니다."));
        } catch (Exception e) {
            log.error("채팅 세션 생성 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("채팅 세션 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 채팅 세션 상세 조회
     */
//    @GetMapping("/sessions/detail/{sessionUuid}")
//    @Transactional(readOnly = true)
//    public ResponseEntity<ApiResponse<ChatSessionResponseDto>> getSessionDetail(@PathVariable UUID sessionUuid) {
//        try {
//            ChatSession session = chatSessionService.findById(sessionUuid);
//            ChatSessionResponseDto response = chatSessionService.toResponseDto(session);
//            return ResponseEntity.ok(ApiResponse.success(response, "채팅 세션을 조회했습니다."));
//        } catch (Exception e) {
//            log.error("세션 상세 조회 실패 - sessionUuid: {}", sessionUuid, e);
//            return ResponseEntity.ok(ApiResponse.error("세션을 찾을 수 없습니다: " + e.getMessage()));
//        }
//    }

    /**
     * 메시지 전송 및 AI 응답 생성 - 자동 키워드 추출 추가
     */
    @PostMapping("/users/{userUuid}/subjects/{subjectUuid}/sessions/{sessionUuid}/messages")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMessage(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid,
            @PathVariable UUID sessionUuid,
            @RequestBody MessageRequest messageRequest) {

        try {
            // 입력 검증
            if (messageRequest == null || messageRequest.getContent() == null || messageRequest.getContent().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("메시지 내용은 필수입니다."));
            }

            if (messageRequest.getSender() == null || messageRequest.getSender().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("발신자 정보는 필수입니다."));
            }

            ChatSession session = chatSessionService.findById(sessionUuid);

            // 1. 사용자 메시지 추가 (교육적 내용 판단)
            boolean isEducational = isEducationalContent(messageRequest.getContent());

            session.addMessage(messageRequest.getSender().toUpperCase(), messageRequest.getContent().trim());

            // 2. 사용자 메시지면 AI 응답 생성
            if ("USER".equalsIgnoreCase(messageRequest.getSender().trim())) {
                try {
                    String subjectName = session.getSubject() != null ?
                            session.getSubject().getTitle() : "일반학습";

                    // AI 응답 생성 - null 안전성 추가
                    String aiResponse = null;
                    try {
                        aiResponse = aiService.generateResponse(messageRequest.getContent(), subjectName);
                    } catch (Exception aiError) {
                        log.error("AI 응답 생성 중 오류", aiError);
                        aiResponse = "죄송합니다. 현재 AI 서비스에 문제가 있습니다. 잠시 후 다시 시도해주세요.";
                    }

                    // AI 메시지 추가
                    if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                        session.addMessage("AI", aiResponse);
                        log.info("AI 응답 생성 완료 - sessionUuid: {}", sessionUuid);

                        // 3. 자동 키워드 추출 (사용자 메시지와 AI 응답 모두 분석)
                        if (isEducational) {
                            try {
                                // 사용자 메시지에서 키워드 추출
                                extractAndSaveKeywords(sessionUuid, messageRequest.getContent(), subjectName);

                                // AI 응답에서도 키워드 추출
                                extractAndSaveKeywords(sessionUuid, aiResponse, subjectName);

                                log.debug("자동 키워드 추출 완료 - sessionUuid: {}", sessionUuid);
                            } catch (Exception keywordError) {
                                log.warn("키워드 추출 실패 (계속 진행) - sessionUuid: {}", sessionUuid, keywordError);
                                // 키워드 추출 실패해도 채팅은 계속 진행
                            }
                        }
                    } else {
                        session.addMessage("AI", "응답을 생성할 수 없습니다.");
                    }

                } catch (Exception e) {
                    log.error("AI 응답 생성 실패 - sessionUuid: {}", sessionUuid, e);
                    // AI 오류 시 에러 메시지 추가
                    session.addMessage("AI", "죄송합니다. 현재 AI 서비스에 문제가 있습니다.");
                }
            }

            ChatSession updatedSession = chatSessionService.save(session);

            Map<String, Object> result = Map.of(
                    "sessionId", sessionUuid,
                    "messageCount", updatedSession.getMessageCount(),
                    "lastUpdated", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponse.success(result, "메시지가 전송되었습니다."));

        } catch (Exception e) {
            log.error("메시지 전송 실패 - sessionUuid: {}", sessionUuid, e);
            return ResponseEntity.ok(ApiResponse.error("메시지 전송에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 세션 제목 수정
     */
    @PutMapping("/sessions/{sessionUuid}")
    @Transactional
    public ResponseEntity<ApiResponse<ChatSession>> updateSessionTitle(
            @PathVariable UUID sessionUuid,
            @RequestBody Map<String, String> request) {

        try {
            String newTitle = request.get("title");
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("새 제목은 필수입니다."));
            }

            if (newTitle.length() > 100) {
                return ResponseEntity.ok(ApiResponse.error("제목은 100자를 초과할 수 없습니다."));
            }

            ChatSession session = chatSessionService.findById(sessionUuid);
            session.setTitle(newTitle.trim());

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
    @Transactional
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
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ChatSessionResponseDto>>> getSessionsBySubject(
            @PathVariable UUID userUuid,
            @PathVariable UUID subjectUuid) {

        try {
            List<ChatSession> sessions = chatSessionService.findByUserAndSubject(userUuid, subjectUuid);
            List<ChatSessionResponseDto> response = sessions.stream()
                    .map(chatSessionService::toResponseDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(response, "과목별 세션 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("과목별 세션 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목별 세션 조회에 실패했습니다: " + e.getMessage()));
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
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success(testData, "채팅 API 연결 성공!"));
    }

    /**
     * 세션 키워드 조회 (디버깅용)
     */
//    @GetMapping("/sessions/{sessionUuid}/keywords")
//    @Transactional(readOnly = true)
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionKeywords(@PathVariable UUID sessionUuid) {
//        try {
//            List<String> keywords = chatSessionService.getExtractedKeywords(sessionUuid);
//            boolean canGenerateProblems = chatSessionService.canGenerateProblems(sessionUuid);
//
//            Map<String, Object> keywordInfo = new HashMap<>();
//            keywordInfo.put("sessionUuid", sessionUuid);
//            keywordInfo.put("keywords", keywords);
//            keywordInfo.put("keywordCount", keywords.size());
//            keywordInfo.put("canGenerateProblems", canGenerateProblems);
//            keywordInfo.put("minKeywordsRequired", 3);
//
//            return ResponseEntity.ok(ApiResponse.success(keywordInfo, "세션 키워드 정보를 조회했습니다."));
//
//        } catch (Exception e) {
//            log.error("세션 키워드 조회 실패 - sessionUuid: {}", sessionUuid, e);
//            return ResponseEntity.ok(ApiResponse.error("키워드 조회에 실패했습니다: " + e.getMessage()));
//        }
//    }

}