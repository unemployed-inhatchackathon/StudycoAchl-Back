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
public class MaterialSummaryService {

    private final OpenAiService openAiService;

    @Value("${app.use-real-ai:true}")
    private boolean useRealAi;

    /**
     * 텍스트를 AI로 요약
     */
    public String generateSummary(String text, String subjectName) {
        if (text == null || text.trim().isEmpty()) {
            return "요약할 내용이 없습니다.";
        }

        if (!useRealAi) {
            return generateMockSummary(text, subjectName);
        }

        try {
            String prompt = createSummaryPrompt(text, subjectName);
            return callOpenAI(prompt);
        } catch (Exception e) {
            log.error("AI 요약 생성 실패", e);
            return "요약 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 핵심 키워드 추출
     */
    public String extractKeywords(String text, String subjectName) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        if (!useRealAi) {
            return generateMockKeywords(subjectName);
        }

        try {
            String prompt = createKeywordPrompt(text, subjectName);
            return callOpenAI(prompt);
        } catch (Exception e) {
            log.error("키워드 추출 실패", e);
            return subjectName + ", 학습내용, 핵심개념";
        }
    }

    /**
     * 학습 포인트 생성
     */
    public String generateLearningPoints(String summary, String subjectName) {
        if (summary == null || summary.trim().isEmpty()) {
            return "학습 포인트를 생성할 수 없습니다.";
        }

        if (!useRealAi) {
            return generateMockLearningPoints(subjectName);
        }

        try {
            String prompt = createLearningPointsPrompt(summary, subjectName);
            return callOpenAI(prompt);
        } catch (Exception e) {
            log.error("학습 포인트 생성 실패", e);
            return "학습 포인트 생성 중 오류가 발생했습니다.";
        }
    }

    // ========== Private Methods ==========

    /**
     * 요약 프롬프트 생성
     */
    private String createSummaryPrompt(String text, String subjectName) {
        return String.format("""
            다음 %s 과목의 학습자료를 한국어로 요약해주세요.
            주요 개념, 핵심 내용, 중요한 포인트를 중심으로 간결하고 이해하기 쉽게 정리해주세요.
            
            학습자료 내용:
            %s
            
            요약 형식:
            ## 주요 개념
            - 개념 1: 설명
            - 개념 2: 설명
            - 개념 3: 설명
            
            ## 핵심 내용
            [3-4 문단으로 핵심 내용 요약]
            
            ## 중요 포인트
            - 포인트 1
            - 포인트 2  
            - 포인트 3
            """, subjectName, text);
    }

    /**
     * 키워드 추출 프롬프트 생성
     */
    private String createKeywordPrompt(String text, String subjectName) {
        return String.format("""
            다음 %s 과목의 학습자료에서 문제 출제에 활용할 수 있는 핵심 키워드들을 추출해주세요.
            개념, 용어, 공식, 이론 등 학습에 중요한 요소들을 중심으로 추출해주세요.
            
            학습자료:
            %s
            
            키워드만 쉼표로 구분하여 나열해주세요 (최대 10개):
            """, subjectName, text);
    }

    /**
     * 학습 포인트 프롬프트 생성
     */
    private String createLearningPointsPrompt(String summary, String subjectName) {
        return String.format("""
            다음 %s 과목의 요약 내용을 바탕으로 학습 시 주의할 점과 포인트를 정리해주세요.
            
            요약 내용:
            %s
            
            학습 포인트 형식:
            ## 학습 시 주의사항
            - 주의사항 1
            - 주의사항 2
            
            ## 암기해야 할 내용
            - 암기 내용 1
            - 암기 내용 2
            
            ## 이해가 필요한 개념
            - 개념 1
            - 개념 2
            """, subjectName, summary);
    }

    /**
     * OpenAI API 호출
     */
    private String callOpenAI(String prompt) {
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                    "당신은 교육 전문가입니다. 학습자료를 분석하여 학생들이 이해하기 쉽도록 요약하고 정리해주세요."),
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
            log.error("OpenAI API 호출 실패", e);
            throw new RuntimeException("AI 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모의 요약 생성 (개발/테스트용)
     */
    private String generateMockSummary(String text, String subjectName) {
        return String.format("""
            ## 주요 개념
            - %s 관련 핵심 개념 1
            - %s 관련 핵심 개념 2
            - %s 관련 핵심 개념 3
            
            ## 핵심 내용
            이 문서는 %s 과목의 학습자료입니다. 
            주요 내용은 다음과 같습니다.
            
            실제 환경에서는 OpenAI API를 통해 정확한 요약을 제공합니다.
            
            ## 중요 포인트
            - 이론적 이해가 중요함
            - 실습을 통한 적용 필요
            - 관련 개념들 간의 연결 이해
            """, subjectName, subjectName, subjectName, subjectName);
    }

    /**
     * 모의 키워드 생성
     */
    private String generateMockKeywords(String subjectName) {
        return String.format("%s, 핵심개념, 이론, 실습, 응용, 기본원리", subjectName);
    }

    /**
     * 모의 학습 포인트 생성
     */
    private String generateMockLearningPoints(String subjectName) {
        return String.format("""
            ## 학습 시 주의사항
            - %s의 기본 개념을 확실히 이해하고 넘어가기
            - 예제와 실습을 통해 이론 적용하기
            
            ## 암기해야 할 내용
            - 핵심 공식 및 정의
            - 중요한 용어와 개념
            
            ## 이해가 필요한 개념
            - 기본 원리와 응용 방법
            - 관련 이론들 간의 연결점
            """, subjectName);
    }

    /**
     * 요약 품질 검증
     */
    public boolean isValidSummary(String summary) {
        return summary != null &&
                !summary.trim().isEmpty() &&
                !summary.contains("오류가 발생했습니다") &&
                summary.length() > 50;
    }
}