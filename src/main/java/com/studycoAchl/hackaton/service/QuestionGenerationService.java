package com.studycoAchl.hackaton.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public QuestionGenerationService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    /**
     * 메인 메서드: 키워드와 컨텍스트 기반 문제 생성 (JSON 형태로 반환)
     */
    public String generateQuestionsJson(List<String> keywords, String context, int questionCount) {
        try {
            log.info("OpenAI로 문제 생성 시작 - keywords: {}, count: {}", keywords, questionCount);

            String prompt = createAdvancedPrompt(keywords, context, questionCount);

            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(Arrays.asList(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                    "당신은 전문 교육 문제 출제자입니다. 주어진 키워드와 맥락을 바탕으로 고품질의 객관식 문제를 생성해주세요. 반드시 요청된 형식으로 응답해야 합니다."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .maxTokens(3000)
                    .temperature(0.7)
                    .build();

            String response = openAiService.createChatCompletion(chatRequest)
                    .getChoices().get(0).getMessage().getContent();

            log.info("OpenAI 응답 받음, 파싱 시작...");

            // ★★★ OpenAI 응답 로그 추가 ★★★
            log.info("=== OpenAI 원본 응답 시작 ===");
            log.info(response);
            log.info("=== OpenAI 원본 응답 끝 ===");

            // OpenAI 응답을 JSON 형태로 파싱
            return parseOpenAIResponse(response, keywords, questionCount);

        } catch (Exception e) {
            log.error("OpenAI 문제 생성 실패", e);
            throw new RuntimeException("AI 문제 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 고급 프롬프트 생성 (더 명확한 지침 추가)
     */
    private String createAdvancedPrompt(List<String> keywords, String context, int questionCount) {
        String keywordText = String.join(", ", keywords);

        return String.format("""
                **문제 생성 요청**
                
                키워드: %s
                컨텍스트: %s
                문제 수: %d개
                
                위 정보를 바탕으로 교육용 객관식 문제를 생성해주세요.
                
                **요구사항:**
                1. 각 문제는 5지선다 형식
                2. 난이도는 중급 수준
                3. 명확한 정답과 해설 포함
                4. 오답 선택지도 그럴듯하게 구성
                5. 한국어로 작성
                
                **응답 형식 (정확히 이 형태로 응답):**
                
                문제 1:
                질문: [문제 내용]
                선택지:
                1) [선택지1]
                2) [선택지2]
                3) [선택지3]
                4) [선택지4]
                5) [선택지5]
                정답: [정답번호]
                해설: [상세한 해설]
                키워드: [관련 키워드]
                
                문제 2:
                질문: [문제 내용]
                선택지:
                1) [선택지1]
                2) [선택지2]
                3) [선택지3]
                4) [선택지4]
                5) [선택지5]
                정답: [정답번호]
                해설: [상세한 해설]
                키워드: [관련 키워드]
                
                (이런 식으로 %d개 문제 생성)
                
                반드시 위 형식을 정확히 따라주세요.
                """, keywordText, context, questionCount, questionCount);
    }

    /**
     * OpenAI 응답을 JSON으로 파싱 (개선된 버전)
     */
    private String parseOpenAIResponse(String response, List<String> keywords, int questionCount) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("title", "AI 생성 문제");
            root.put("totalQuestions", questionCount);
            root.put("keywords", String.join(", ", keywords));
            root.put("createdAt", LocalDateTime.now().toString());
            root.put("source", "OpenAI GPT-3.5-turbo");

            ArrayNode questionsArray = objectMapper.createArrayNode();

            // ★★★ 더 유연한 파싱 로직 ★★★
            List<ParsedQuestion> parsedQuestions = parseQuestionsFromResponse(response);

            log.info("파싱 시도 결과: {}개 문제 발견", parsedQuestions.size());

            // 파싱된 문제가 없다면 응답 전체를 분석해서 강제로 문제 생성
            if (parsedQuestions.isEmpty()) {
                log.warn("정규 파싱 실패, 대안 파싱 시도...");
                parsedQuestions = tryAlternativeParsing(response, keywords);
            }

            for (int i = 0; i < Math.min(parsedQuestions.size(), questionCount); i++) {
                ParsedQuestion pq = parsedQuestions.get(i);
                ObjectNode question = objectMapper.createObjectNode();

                question.put("id", i + 1);
                question.put("question", pq.questionText);

                ArrayNode options = objectMapper.createArrayNode();
                for (String option : pq.options) {
                    options.add(option);
                }
                question.set("options", options);

                question.put("correctAnswer", pq.correctAnswer - 1); // 0-based index로 변환
                question.put("explanation", pq.explanation);
                question.put("difficulty", "보통");
                question.put("timeLimit", 45);
                question.put("hint", "키워드: " + pq.keyword);
                question.put("keyword", pq.keyword);

                questionsArray.add(question);
            }

            // 여전히 문제가 없다면 기본 문제 생성
            if (parsedQuestions.size() < questionCount) {
                log.warn("파싱된 문제 수 부족: {}/{}, 기본 문제로 보완", parsedQuestions.size(), questionCount);
                for (int i = parsedQuestions.size(); i < questionCount; i++) {
                    questionsArray.add(createFallbackQuestion(i + 1, keywords));
                }
            }

            root.set("questions", questionsArray);
            String result = objectMapper.writeValueAsString(root);
            log.info("JSON 파싱 완료, 문제 {}개 생성됨", questionsArray.size());
            return result;

        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패", e);
            throw new RuntimeException("AI 응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 문제 파싱을 위한 정규식 처리 (개선된 버전)
     */
    private List<ParsedQuestion> parseQuestionsFromResponse(String response) {
        List<ParsedQuestion> questions = new ArrayList<>();

        try {
            // 다양한 패턴으로 문제 분할 시도
            String[] problemBlocks = null;

            // 패턴 1: "문제 1:", "문제 2:" 등
            if (response.contains("문제 ")) {
                problemBlocks = response.split("문제 \\d+:");
                log.debug("패턴 1 사용: '문제 X:' 형태로 분할");
            }
            // 패턴 2: "1.", "2." 등
            else if (response.contains("1.") && response.contains("2.")) {
                problemBlocks = response.split("\\d+\\.");
                log.debug("패턴 2 사용: 'X.' 형태로 분할");
            }
            // 패턴 3: 줄바꿈으로 구분된 블록
            else {
                problemBlocks = response.split("\\n\\n");
                log.debug("패턴 3 사용: 줄바꿈으로 분할");
            }

            if (problemBlocks != null) {
                for (int i = 1; i < problemBlocks.length; i++) { // 첫 번째는 보통 빈 문자열
                    try {
                        ParsedQuestion pq = parseSingleQuestion(problemBlocks[i]);
                        if (pq != null) {
                            questions.add(pq);
                            log.debug("문제 {} 파싱 성공: {}", i,
                                    pq.questionText.substring(0, Math.min(50, pq.questionText.length())));
                        }
                    } catch (Exception e) {
                        log.warn("문제 {} 파싱 실패", i, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("전체 파싱 과정에서 오류", e);
        }

        log.info("총 {}개 문제 파싱 완료", questions.size());
        return questions;
    }

    /**
     * 단일 문제 파싱 (더 유연하게 개선)
     */
    private ParsedQuestion parseSingleQuestion(String questionBlock) {
        try {
            ParsedQuestion pq = new ParsedQuestion();

            // 질문 추출 (여러 패턴 시도)
            String questionText = extractQuestionText(questionBlock);
            if (questionText == null || questionText.isEmpty()) {
                return null;
            }
            pq.questionText = questionText;

            // 선택지 추출 (여러 패턴 시도)
            List<String> options = extractOptions(questionBlock);
            if (options.size() < 2) {
                return null;
            }
            pq.options = options;

            // 정답 추출
            int correctAnswer = extractCorrectAnswer(questionBlock, options.size());
            if (correctAnswer <= 0) {
                correctAnswer = 1; // 기본값
            }
            pq.correctAnswer = correctAnswer;

            // 해설 추출
            pq.explanation = extractExplanation(questionBlock);
            if (pq.explanation == null || pq.explanation.isEmpty()) {
                pq.explanation = "해설이 제공되지 않았습니다.";
            }

            // 키워드 추출
            pq.keyword = extractKeyword(questionBlock);
            if (pq.keyword == null || pq.keyword.isEmpty()) {
                pq.keyword = "일반";
            }

            return pq;

        } catch (Exception e) {
            log.warn("개별 문제 파싱 중 오류", e);
            return null;
        }
    }

    /**
     * 질문 텍스트 추출 (여러 패턴 지원)
     */
    private String extractQuestionText(String block) {
        // 패턴 1: "질문:" 으로 시작
        Pattern pattern1 = Pattern.compile("질문:\\s*(.+?)(?=선택지:|\\d+\\)|$)", Pattern.DOTALL);
        Matcher matcher1 = pattern1.matcher(block);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // 패턴 2: 첫 번째 줄이 질문
        String[] lines = block.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && (line.contains("?") || line.length() > 10)) {
                return line;
            }
        }

        // 패턴 3: 전체를 질문으로 간주
        if (block.trim().length() > 10) {
            return block.trim().substring(0, Math.min(200, block.trim().length()));
        }

        return null;
    }

    /**
     * 선택지 추출 (여러 패턴 지원)
     */
    private List<String> extractOptions(String block) {
        List<String> options = new ArrayList<>();

        // 패턴 1: "1) ", "2) " 형태
        Pattern pattern1 = Pattern.compile("\\d+\\)\\s*(.+?)(?=\\d+\\)|정답:|해설:|$)", Pattern.DOTALL);
        Matcher matcher1 = pattern1.matcher(block);
        while (matcher1.find()) {
            String option = matcher1.group(1).trim();
            if (!option.isEmpty()) {
                options.add(option);
            }
        }

        // 패턴 2: "①", "②" 형태
        if (options.isEmpty()) {
            Pattern pattern2 = Pattern.compile("[①②③④⑤]\\s*(.+?)(?=[①②③④⑤]|정답:|해설:|$)", Pattern.DOTALL);
            Matcher matcher2 = pattern2.matcher(block);
            while (matcher2.find()) {
                String option = matcher2.group(1).trim();
                if (!option.isEmpty()) {
                    options.add(option);
                }
            }
        }

        // 기본 선택지가 없으면 더미 생성
        if (options.isEmpty()) {
            options.add("선택지 1");
            options.add("선택지 2");
            options.add("선택지 3");
            options.add("선택지 4");
            options.add("선택지 5");
        }

        return options;
    }

    /**
     * 정답 추출
     */
    private int extractCorrectAnswer(String block, int optionCount) {
        Pattern pattern = Pattern.compile("정답:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            int answer = Integer.parseInt(matcher.group(1));
            if (answer >= 1 && answer <= optionCount) {
                return answer;
            }
        }
        return 1; // 기본값
    }

    /**
     * 해설 추출
     */
    private String extractExplanation(String block) {
        Pattern pattern = Pattern.compile("해설:\\s*(.+?)(?=키워드:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "상세한 해설이 제공되지 않았습니다.";
    }

    /**
     * 키워드 추출
     */
    private String extractKeyword(String block) {
        Pattern pattern = Pattern.compile("키워드:\\s*(.+?)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "일반";
    }

    /**
     * 대안 파싱 방법 (정규 파싱이 실패했을 때)
     */
    private List<ParsedQuestion> tryAlternativeParsing(String response, List<String> keywords) {
        List<ParsedQuestion> questions = new ArrayList<>();

        try {
            log.info("대안 파싱 시작 - 응답 길이: {}", response.length());

            // 응답에서 질문처럼 보이는 부분을 찾아서 강제로 문제 생성
            if (response.length() > 50) {
                ParsedQuestion pq = new ParsedQuestion();

                // 응답의 첫 부분을 질문으로 사용
                String[] sentences = response.split("[.?!]");
                String questionText = sentences.length > 0 ? sentences[0] : response.substring(0, Math.min(100, response.length()));

                pq.questionText = "AI 생성 문제: " + questionText.trim();
                pq.options = Arrays.asList("선택지 1", "선택지 2 (정답)", "선택지 3", "선택지 4", "선택지 5");
                pq.correctAnswer = 2;
                pq.explanation = "OpenAI가 생성한 내용을 기반으로 한 문제입니다: " + response.substring(0, Math.min(200, response.length()));
                pq.keyword = keywords.isEmpty() ? "AI생성" : keywords.get(0);

                questions.add(pq);
                log.info("대안 파싱으로 1개 문제 생성 완료");
            }

        } catch (Exception e) {
            log.warn("대안 파싱도 실패", e);
        }

        return questions;
    }

    /**
     * 파싱 실패시 대체 문제 생성
     */
    private ObjectNode createFallbackQuestion(int questionNum, List<String> keywords) {
        ObjectNode question = objectMapper.createObjectNode();
        String keyword = keywords.isEmpty() ? "일반" : keywords.get(0);

        question.put("id", questionNum);
        question.put("question", String.format("문제 %d: %s 관련 질문입니다. (AI 파싱 실패로 인한 대체 문제)", questionNum, keyword));

        ArrayNode options = objectMapper.createArrayNode();
        options.add("선택지 1");
        options.add("선택지 2 (정답)");
        options.add("선택지 3");
        options.add("선택지 4");
        options.add("선택지 5");
        question.set("options", options);

        question.put("correctAnswer", 1); // 0-based index
        question.put("explanation", "AI 파싱 실패로 인한 대체 문제입니다. 실제 서비스에서는 다시 생성해주세요.");
        question.put("difficulty", "보통");
        question.put("timeLimit", 30);
        question.put("hint", "대체 문제입니다.");
        question.put("keyword", keyword);

        return question;
    }

    /**
     * 파싱된 문제 데이터 클래스
     */
    private static class ParsedQuestion {
        String questionText;
        List<String> options;
        int correctAnswer;
        String explanation;
        String keyword;
    }

    /**
     * 레거시 메서드 (기존 코드 호환성을 위해 유지)
     */
    public String generateQuestions(String subject, String content, int questionCount) {
        return generateQuestionsJson(Arrays.asList(subject), content, questionCount);
    }
}