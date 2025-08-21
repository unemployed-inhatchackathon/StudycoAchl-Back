package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.service.ChatProblemIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat-problem")
@RequiredArgsConstructor
@Slf4j
public class ChatProblemController {

    private final ChatProblemIntegrationService chatProblemIntegrationService;

    /**
     * 채팅 기반 문제 생성 (핵심 통합 API)
     */
    @PostMapping("/generate/{chatSessionId}")
    public ApiResponse<Map<String, Object>> generateProblemFromChat(
            @PathVariable UUID chatSessionId,
            @RequestParam(defaultValue = "5") int questionCount) {

        try {
            log.info("채팅 기반 문제 생성 요청 - sessionId: {}, questionCount: {}", chatSessionId, questionCount);

            Map<String, Object> result = chatProblemIntegrationService.generateProblemFromChat(chatSessionId, questionCount);

            if ((Boolean) result.get("success")) {
                return ApiResponse.success(result, "채팅 내용을 바탕으로 문제가 생성되었습니다!");
            } else {
                return ApiResponse.error((String) result.get("error"));
            }

        } catch (Exception e) {
            log.error("채팅 기반 문제 생성 실패 - sessionId: {}", chatSessionId, e);
            return ApiResponse.error("문제 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 실시간 메시지 키워드 분석 (디버깅 버전)
     */
    @PostMapping("/analyze/{chatSessionId}")
    public ApiResponse<Map<String, Object>> analyzeMessage(
            @PathVariable UUID chatSessionId,
            @RequestBody Map<String, String> request) {

        try {
            String messageContent = request.get("content");

            if (messageContent == null || messageContent.trim().isEmpty()) {
                return ApiResponse.error("메시지 내용은 필수입니다.");
            }

            log.debug("실시간 메시지 분석 - sessionId: {}, content: {}", chatSessionId, messageContent);

            // 단계별 디버깅
            log.info("1단계: ChatProblemIntegrationService 호출 시작");

            Map<String, Object> result;
            try {
                result = chatProblemIntegrationService.analyzeMessageForKeywords(chatSessionId, messageContent);
                log.info("2단계: 서비스 호출 완료, result: {}", result);
            } catch (Exception serviceException) {
                log.error("서비스 호출 중 오류 발생", serviceException);
                return ApiResponse.error("서비스 호출 실패: " + serviceException.getMessage());
            }

            // null 체크
            if (result == null) {
                log.error("3단계: result가 null입니다!");
                return ApiResponse.error("서비스에서 null 결과를 반환했습니다.");
            }

            log.info("3단계: result 내용 확인 - keys: {}", result.keySet());

            // success 키 확인
            if (!result.containsKey("success")) {
                log.error("4단계: result에 'success' 키가 없습니다! available keys: {}", result.keySet());
                return ApiResponse.error("서비스 응답에 'success' 필드가 없습니다.");
            }

            Object successValue = result.get("success");
            log.info("4단계: success 값: {} (타입: {})", successValue, successValue != null ? successValue.getClass() : "null");

            // success 값이 Boolean인지 확인
            if (successValue == null) {
                log.error("5단계: success 값이 null입니다!");
                return ApiResponse.error("서비스 응답의 'success' 값이 null입니다.");
            }

            if (!(successValue instanceof Boolean)) {
                log.error("5단계: success 값이 Boolean이 아닙니다! 실제 타입: {}", successValue.getClass());
                return ApiResponse.error("서비스 응답의 'success' 값이 Boolean 타입이 아닙니다.");
            }

            Boolean success = (Boolean) successValue;
            log.info("5단계: Boolean 캐스팅 성공, 값: {}", success);

            if (success) {
                return ApiResponse.success(result, "메시지 분석이 완료되었습니다.");
            } else {
                String errorMessage = (String) result.getOrDefault("error", "알 수 없는 오류");
                return ApiResponse.error(errorMessage);
            }

        } catch (Exception e) {
            log.error("메시지 분석 실패 - sessionId: {}", chatSessionId, e);
            return ApiResponse.error("메시지 분석에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 문제 생성 가능 여부 확인
     */
    @GetMapping("/can-generate/{chatSessionId}")
    public ApiResponse<Map<String, Object>> canGenerateProblems(@PathVariable UUID chatSessionId) {
        try {
            log.debug("문제 생성 가능 여부 확인 - sessionId: {}", chatSessionId);

            boolean canGenerate = chatProblemIntegrationService.canGenerateProblemsFromChat(chatSessionId);

            Map<String, Object> result = Map.of(
                    "sessionId", chatSessionId,
                    "canGenerateProblems", canGenerate,
                    "message", canGenerate ? "문제 생성이 가능합니다." : "더 많은 학습 대화가 필요합니다."
            );

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("문제 생성 가능 여부 확인 실패 - sessionId: {}", chatSessionId, e);
            return ApiResponse.error("확인에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 문제 생성용 키워드 조회
     */
    @GetMapping("/keywords/{chatSessionId}")
    public ApiResponse<Map<String, Object>> getKeywordsForProblemGeneration(@PathVariable UUID chatSessionId) {
        try {
            log.debug("문제 생성용 키워드 조회 - sessionId: {}", chatSessionId);

            List<String> keywords = chatProblemIntegrationService.getKeywordsForProblemGeneration(chatSessionId);

            Map<String, Object> result = Map.of(
                    "sessionId", chatSessionId,
                    "keywords", keywords,
                    "keywordCount", keywords.size(),
                    "suggestedQuestionCount", Math.min(keywords.size() * 2, 10)
            );

            return ApiResponse.success(result, "문제 생성용 키워드를 조회했습니다.");

        } catch (Exception e) {
            log.error("키워드 조회 실패 - sessionId: {}", chatSessionId, e);
            return ApiResponse.error("키워드 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅-문제 통합 상태 확인
     */
    @GetMapping("/status/{chatSessionId}")
    public ApiResponse<Map<String, Object>> getIntegrationStatus(@PathVariable UUID chatSessionId) {
        try {
            log.debug("통합 상태 확인 - sessionId: {}", chatSessionId);

            // 통합 상태 정보 수집
            boolean canGenerate = chatProblemIntegrationService.canGenerateProblemsFromChat(chatSessionId);
            List<String> keywords = chatProblemIntegrationService.getKeywordsForProblemGeneration(chatSessionId);

            Map<String, Object> status = Map.of(
                    "sessionId", chatSessionId,
                    "keywordCount", keywords.size(),
                    "canGenerateProblems", canGenerate,
                    "extractedKeywords", keywords,
                    "recommendedActions", canGenerate ?
                            List.of("문제 생성하기", "추가 학습하기") :
                            List.of("더 많은 대화하기", "학습 내용 질문하기")
            );

            return ApiResponse.success(status, "통합 상태를 확인했습니다.");

        } catch (Exception e) {
            log.error("통합 상태 확인 실패 - sessionId: {}", chatSessionId, e);
            return ApiResponse.error("상태 확인에 실패했습니다: " + e.getMessage());
        }
    }
}