package com.studycoAchl.hackaton.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class QuestionGenerationService {

    private final OpenAiService openAiService;

    @Autowired
    public QuestionGenerationService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String generateQuestions(String subject, String content, int questionCount) {
        String prompt = createPrompt(subject, content, questionCount);

        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                "당신은 교육 전문가입니다. 주어진 내용을 바탕으로 객관식 문제를 생성해주세요."),
                        new ChatMessage(ChatMessageRole.USER.value(), prompt)
                ))
                .maxTokens(2000)
                .temperature(0.7)
                .build();

        return openAiService.createChatCompletion(chatRequest)
                .getChoices().get(0).getMessage().getContent();
    }

    private String createPrompt(String subject, String content, int questionCount) {
        return String.format("""
            과목: %s
            학습 내용: %s
            
            위 내용을 바탕으로 %d개의 객관식 문제를 만들어주세요.
            
            각 문제는 다음 형식으로 작성해주세요:
            1. 문제: [문제 내용]
            선택지:
            1) [선택지 1]
            2) [선택지 2] 
            3) [선택지 3]
            4) [선택지 4]
            정답: [정답 번호]
            해설: [정답 해설]
            선택지별 설명:
            1) [선택지 1 설명]
            2) [선택지 2 설명]
            3) [선택지 3 설명]
            4) [선택지 4 설명]
            키워드: [관련 키워드]
            
            """, subject, content, questionCount);
    }

    // 응답 파싱 메서드 (나중에 구현)
    public List<QuestionData> parseGptResponse(String gptResponse) {
        // TODO: GPT 응답을 파싱해서 Question 엔티티로 변환하는 로직
        return null;
    }

    // 임시 데이터 클래스
    public static class QuestionData {
        private String questionText;
        private String[] options;
        private int correctAnswer;
        private String explanation;
        private String[] optionExplanations;
        private String keyword;

        // getters and setters
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public String[] getOptions() { return options; }
        public void setOptions(String[] options) { this.options = options; }
        public int getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(int correctAnswer) { this.correctAnswer = correctAnswer; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String[] getOptionExplanations() { return optionExplanations; }
        public void setOptionExplanations(String[] optionExplanations) { this.optionExplanations = optionExplanations; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }
}