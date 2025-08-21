package com.studycoAchl.hackaton.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final OpenAiService openAiService;

    @Value("${app.use-real-ai:true}")
    private boolean useRealAi;

    // ========== 채팅 응답 생성 ==========

    public String generateResponse(String userMessage, String subjectName) {
        if (!useRealAi) {
            return generateMockResponse(userMessage, subjectName);
        }

        try {
            String systemPrompt = buildSystemPrompt(subjectName);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                            new ChatMessage(ChatMessageRole.USER.value(), userMessage)
                    ))
                    .maxTokens(1500)
                    .temperature(0.3)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생: {}", e.getMessage());
            return "죄송합니다. AI 서비스에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    // ========== 키워드 추출 (통합 기능) ==========

    public String extractKeywords(String conversationText, String subjectName) {
        if (!useRealAi) {
            return generateMockKeywords(conversationText, subjectName);
        }

        try {
            String keywordPrompt = buildKeywordExtractionPrompt(conversationText, subjectName);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.USER.value(), keywordPrompt)
                    ))
                    .maxTokens(500)
                    .temperature(0.1) // 더 일관된 키워드 추출을 위해 낮은 temperature
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .trim();

        } catch (Exception e) {
            log.error("키워드 추출 중 오류 발생: {}", e.getMessage());
            return "";
        }
    }

    // ========== 문제 생성 지원 (통합 기능) ==========

    public String generateCompletion(String prompt) {
        if (!useRealAi) {
            return "모의 AI 응답: " + prompt.substring(0, Math.min(50, prompt.length())) + "...";
        }

        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .maxTokens(2000)
                    .temperature(0.3)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            log.error("AI 완성 생성 중 오류 발생: {}", e.getMessage());
            return "AI 처리 중 오류가 발생했습니다.";
        }
    }

    // ========== 프롬프트 생성 메소드들 ==========

    private String buildSystemPrompt(String subjectName) {
        return String.format(
                "당신은 %s 전문 튜터입니다. " +
                        "학생들의 질문에 정확하고 이해하기 쉽게 답변해주세요. " +
                        "복잡한 개념은 단계별로 설명하고, 적절한 예시를 들어주세요. " +
                        "학생의 학습 수준에 맞춰 설명의 난이도를 조절해주세요.",
                subjectName
        );
    }

    private String buildKeywordExtractionPrompt(String conversationText, String subjectName) {
        return String.format(
                "다음 %s 과목의 대화에서 학습과 관련된 핵심 키워드들을 추출해주세요.\n" +
                        "키워드는 쉼표로 구분하여 나열해주세요.\n" +
                        "개념, 용어, 공식, 이론 등 학습에 중요한 요소들을 중심으로 추출해주세요.\n\n" +
                        "대화 내용:\n%s\n\n" +
                        "추출된 키워드:",
                subjectName, conversationText
        );
    }

    // ========== 모의 응답 생성 (개발/테스트용) ==========

    private String generateMockResponse(String userMessage, String subjectName) {
        return String.format(
                "[모의 AI 응답]\n" +
                        "%s 과목에 대한 질문을 받았습니다: \"%s\"\n" +
                        "실제 운영 환경에서는 OpenAI API를 통해 정확한 답변을 제공합니다.",
                subjectName, userMessage
        );
    }

    private String generateMockKeywords(String conversationText, String subjectName) {
        return String.format("%s, 학습, 개념, 이해", subjectName);
    }
}