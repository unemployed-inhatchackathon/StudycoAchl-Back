package com.studycoAchl.hackaton.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class aiService {
    @Value("openai.api.key")
    private String apiKey;
    private final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String generateResponse(String userMessage, String subjectName) {
        RestTemplate restTemplate = new RestTemplate();

        String systemPrompt = "당신은 " + subjectName + " 전문 튜터입니다. " +
                "학생들의 질문에 정확하고 이해하기 쉽게 답변해주세요. " +
                "복잡한 개념은 단계별로 설명하고, 예시를 들어주세요.";

        // Headers 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Request Body 설정
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("max_tokens", 1500);
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_URL, HttpMethod.POST, entity, Map.class
            );

            // AI 응답 추출
            return extractAIResponse(response.getBody());
        } catch (Exception e) {
            return "죄송합니다. AI 서비스에 문제가 발생했습니다.";
        }
    }
    private String extractAIResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "응답을 처리하는 중 오류가 발생했습니다.";
        }
    }
}