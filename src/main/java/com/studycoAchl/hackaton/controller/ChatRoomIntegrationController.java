package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.service.ChatRoomIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chatroom-integration")
public class ChatRoomIntegrationController {

    @Autowired
    private ChatRoomIntegrationService integrationService;

    // 기본 헬스체크 (GET)
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("채팅방 연동 API 정상 작동!");
    }

    // 간단한 테스트용 문제 생성 (GET)
    @GetMapping("/test-generate")
    public ResponseEntity<Map<String, Object>> testGenerate() {
        try {
            Map<String, Object> result = integrationService.generateProblemsFromChatContent(
                    "test-user-uuid",
                    "test-subject-uuid",
                    "test-chat-uuid",
                    "수학 덧셈과 뺄셈에 대해 배우고 있습니다",
                    3
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // 채팅방에서 문제 생성 요청 (POST)
    @PostMapping("/generate-problems")
    public ResponseEntity<Map<String, Object>> generateProblemsForChatRoom(
            @RequestBody GenerateProblemRequest request) {

        try {
            Map<String, Object> result = integrationService.generateProblemsFromChatContent(
                    request.getUserUuid(),
                    request.getSubjectUuid(),
                    request.getChatSessionUuid(),
                    request.getChatContent(),
                    request.getQuestionCount()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // 채팅방에서 기존 문제 조회
    @GetMapping("/get-chat-problems/{chatSessionUuid}")
    public ResponseEntity<Map<String, Object>> getChatProblems(
            @PathVariable String chatSessionUuid) {

        try {
            Map<String, Object> result = integrationService.getChatSessionProblems(chatSessionUuid);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // 채팅방에서 문제 답안 제출
    @PostMapping("/submit-answer")
    public ResponseEntity<Map<String, Object>> submitAnswerFromChat(
            @RequestBody SubmitAnswerRequest request) {

        try {
            Map<String, Object> result = integrationService.processAnswerSubmission(
                    request.getProblemUuid(),
                    request.getQuestionId(),
                    request.getSelectedAnswer(),
                    request.getUserUuid()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}

// 요청 DTO들
class GenerateProblemRequest {
    private String userUuid;
    private String subjectUuid;
    private String chatSessionUuid;
    private String chatContent;  // 채팅 내용
    private Integer questionCount = 3; // 기본 3문제

    // getters and setters
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public String getSubjectUuid() { return subjectUuid; }
    public void setSubjectUuid(String subjectUuid) { this.subjectUuid = subjectUuid; }
    public String getChatSessionUuid() { return chatSessionUuid; }
    public void setChatSessionUuid(String chatSessionUuid) { this.chatSessionUuid = chatSessionUuid; }
    public String getChatContent() { return chatContent; }
    public void setChatContent(String chatContent) { this.chatContent = chatContent; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
}

class SubmitAnswerRequest {
    private String problemUuid;
    private Integer questionId;
    private Integer selectedAnswer;
    private String userUuid;

    // getters and setters
    public String getProblemUuid() { return problemUuid; }
    public void setProblemUuid(String problemUuid) { this.problemUuid = problemUuid; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Integer getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(Integer selectedAnswer) { this.selectedAnswer = selectedAnswer; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
}