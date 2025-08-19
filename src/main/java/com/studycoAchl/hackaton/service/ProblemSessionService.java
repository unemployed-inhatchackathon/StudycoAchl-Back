package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.dto.CurrentQuestionResponse;
import com.studycoAchl.hackaton.dto.SessionStatusResponse;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.repository.ChatSessionRepository;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ProblemSessionService {

    private static final Logger log = LoggerFactory.getLogger(ProblemSessionService.class);

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 문제풀이 세션 생성 (키워드 기반)
     */
    public Map<String, Object> createProblemSession(String title, String userUuid,
                                                    String subjectUuid, int questionCount,
                                                    String difficulty, String category) {
        try {
            log.info("문제풀이 세션 생성 시작 - title: {}, questionCount: {}", title, questionCount);

            // 1. 새로운 채팅 세션 생성
            String sessionId = UUID.randomUUID().toString();
            ChatSession chatSession = ChatSession.builder()
                    .uuid(sessionId)
                    .title(title)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .status(ChatSession.SessionStatus.ACTIVE)
                    .generatedProblemCount(0)
                    .messages(new ArrayList<>()) // 빈 리스트로 초기화
                    .build();

            // 2. 세션 메타데이터를 extractedKeywords 필드에 임시 저장
            Map<String, Object> sessionMetadata = new HashMap<>();
            sessionMetadata.put("status", "WAITING");
            sessionMetadata.put("currentQuestionIndex", 0);
            sessionMetadata.put("totalQuestions", questionCount);
            sessionMetadata.put("participantCount", 1);
            sessionMetadata.put("difficulty", difficulty);
            sessionMetadata.put("category", category);

            // 메타데이터를 JSON으로 변환하여 임시 저장
            String metadataJson = objectMapper.writeValueAsString(sessionMetadata);
            chatSession.setExtractedKeywords(metadataJson);

            // 3. 데이터베이스에 저장
            ChatSession savedSession = chatSessionRepository.save(chatSession);

            // 4. 임시 문제 데이터 생성 (기존 DB 구조에 맞춰서)
            Problem problem = createMockProblem(sessionId, userUuid, subjectUuid, questionCount);
            problemRepository.save(problem);

            // 5. 응답 생성
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sessionId", sessionId);
            result.put("title", title);
            result.put("questionCount", questionCount);
            result.put("difficulty", difficulty);
            result.put("category", category);
            result.put("message", "세션이 성공적으로 생성되었습니다.");

            log.info("문제풀이 세션 생성 완료 - sessionId: {}", sessionId);
            return result;

        } catch (Exception e) {
            log.error("문제풀이 세션 생성 실패", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "세션 생성에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 임시 문제 데이터 생성 (기존 DB 구조에 맞춰서)
     */
    private Problem createMockProblem(String sessionId, String userUuid, String subjectUuid, int questionCount) {
        try {
            // 기존 DB 구조에 맞는 문제 데이터 생성
            Map<String, Object> problemData = new HashMap<>();
            problemData.put("title", "AI 생성 문제");
            problemData.put("source", "OpenAI GPT-3.5-turbo");
            problemData.put("keywords", "일반학습, 종합문제, 기본지식");
            problemData.put("createdAt", LocalDateTime.now().toString());
            problemData.put("totalQuestions", questionCount);

            List<Map<String, Object>> questions = new ArrayList<>();

            // 샘플 문제들 생성
            for (int i = 0; i < questionCount; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i + 1);
                question.put("question", "문제 " + (i + 1) + ": 다음 중 올바른 답은?");
                question.put("options", Arrays.asList("선택지 1", "선택지 2", "선택지 3", "선택지 4", "선택지 5"));
                question.put("correctAnswer", 0);
                question.put("difficulty", "보통");
                question.put("timeLimit", 45);
                question.put("hint", "힌트: 첫 번째 선택지를 고려해보세요.");
                question.put("keyword", "일반학습, 종합문제");
                question.put("explanation", "이것은 일반적인 학습 문제입니다.");
                questions.add(question);
            }

            problemData.put("questions", questions);

            String problemsJson = objectMapper.writeValueAsString(problemData);

            // Problem 엔티티 생성
            Problem problem = new Problem();
            problem.setUuid(UUID.randomUUID().toString());
            problem.setProblems(problemsJson);
            problem.setUserUuid(userUuid);
            problem.setSubjectUuid(subjectUuid);
            problem.setChatSessionUuid(sessionId);
            problem.setCreatedData(LocalDateTime.now());

            return problem;

        } catch (Exception e) {
            log.error("임시 문제 생성 실패", e);
            throw new RuntimeException("문제 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 문제 조회
     */
    public CurrentQuestionResponse getCurrentQuestion(String sessionId) {
        log.info("현재 문제 조회 시작 - sessionId: {}", sessionId);

        // 1. 채팅 세션 조회
        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            log.error("세션을 찾을 수 없음 - sessionId: {}", sessionId);
            throw new RuntimeException("세션을 찾을 수 없습니다. ID: " + sessionId);
        }

        ChatSession session = sessionOpt.get();

        // 2. 해당 세션의 문제 조회
        Optional<Problem> problemOpt = problemRepository.findByChatSessionUuid(sessionId);
        if (!problemOpt.isPresent()) {
            throw new RuntimeException("세션에 연결된 문제를 찾을 수 없습니다.");
        }

        Problem problem = problemOpt.get();
        String problemsJson = problem.getProblems();

        if (problemsJson == null || problemsJson.isEmpty()) {
            throw new RuntimeException("문제 데이터가 없습니다.");
        }

        try {
            // 3. JSON에서 문제 데이터 파싱
            JsonNode problemsNode = objectMapper.readTree(problemsJson);

            // 현재 문제 인덱스 가져오기
            int currentQuestionIndex = getCurrentQuestionIndex(session);

            // 문제 배열에서 현재 문제 선택
            JsonNode questionsArray = problemsNode.get("questions");
            if (questionsArray == null || !questionsArray.isArray()) {
                throw new RuntimeException("문제 배열을 찾을 수 없습니다.");
            }

            if (currentQuestionIndex >= questionsArray.size()) {
                throw new RuntimeException("모든 문제가 완료되었습니다.");
            }

            JsonNode currentQuestion = questionsArray.get(currentQuestionIndex);

            // 4. 응답 객체 생성
            CurrentQuestionResponse response = new CurrentQuestionResponse();
            response.setQuestionId(problem.getUuid() + "_" + currentQuestionIndex);
            response.setQuestionText(currentQuestion.path("question").asText());

            // 선택지 파싱
            JsonNode optionsNode = currentQuestion.get("options");
            List<String> options = new ArrayList<>();
            if (optionsNode != null && optionsNode.isArray()) {
                for (JsonNode option : optionsNode) {
                    options.add(option.asText());
                }
            }
            response.setOptions(options);

            response.setQuestionNumber(currentQuestionIndex + 1);
            response.setTotalQuestions(questionsArray.size());
            response.setDifficulty(currentQuestion.path("difficulty").asText("보통"));

            // 과목 정보 설정
            Optional<Subject> subjectOpt = subjectRepository.findById(problem.getSubjectUuid());
            if (subjectOpt.isPresent()) {
                response.setCategory(subjectOpt.get().getTitle());
            } else {
                response.setCategory("일반");
            }

            // 시간 제한과 힌트 정보
            response.setTimeLimit(currentQuestion.path("timeLimit").asInt(30));
            response.setQuestionStartTime(LocalDateTime.now());
            response.setHasHint(currentQuestion.has("hint") &&
                    !currentQuestion.path("hint").asText().isEmpty());

            log.info("현재 문제 조회 완료 - sessionId: {}, questionNumber: {}",
                    sessionId, response.getQuestionNumber());

            return response;

        } catch (Exception e) {
            log.error("문제 데이터 파싱 실패 - sessionId: {}", sessionId, e);
            throw new RuntimeException("문제 데이터를 파싱할 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 문제 인덱스 조회 (extractedKeywords에서 메타데이터 파싱)
     */
    private int getCurrentQuestionIndex(ChatSession session) {
        String metadataJson = session.getExtractedKeywords();

        if (metadataJson == null || metadataJson.isEmpty()) {
            return 0; // 기본값: 첫 번째 문제
        }

        try {
            JsonNode metadataNode = objectMapper.readTree(metadataJson);
            return metadataNode.path("currentQuestionIndex").asInt(0);
        } catch (Exception e) {
            log.warn("현재 문제 인덱스 파싱 실패, 기본값 사용 - sessionId: {}", session.getUuid());
            return 0;
        }
    }

    /**
     * 세션 상태 조회
     */
    public SessionStatusResponse getSessionStatus(String sessionId) {
        log.info("세션 상태 조회 시작 - sessionId: {}", sessionId);

        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new RuntimeException("세션을 찾을 수 없습니다.");
        }

        ChatSession session = sessionOpt.get();

        SessionStatusResponse response = new SessionStatusResponse();
        response.setSessionId(session.getUuid());

        // 세션에 연결된 문제 정보 조회
        Optional<Problem> problemOpt = problemRepository.findByChatSessionUuid(sessionId);

        if (problemOpt.isPresent()) {
            Problem problem = problemOpt.get();
            try {
                String problemsJson = problem.getProblems();
                if (problemsJson != null && !problemsJson.isEmpty()) {
                    JsonNode problemsNode = objectMapper.readTree(problemsJson);
                    JsonNode questionsArray = problemsNode.get("questions");

                    if (questionsArray != null && questionsArray.isArray()) {
                        response.setTotalQuestions(questionsArray.size());
                    }
                }

                // 과목 정보
                Optional<Subject> subjectOpt = subjectRepository.findById(problem.getSubjectUuid());
                if (subjectOpt.isPresent()) {
                    response.setSubjectTitle(subjectOpt.get().getTitle());
                } else {
                    response.setSubjectTitle("일반");
                }

            } catch (Exception e) {
                log.warn("문제 정보 파싱 실패 - sessionId: {}", sessionId);
            }
        }

        // extractedKeywords에서 현재 상태 파싱
        try {
            String metadataJson = session.getExtractedKeywords();
            if (metadataJson != null && !metadataJson.isEmpty()) {
                JsonNode metadataNode = objectMapper.readTree(metadataJson);

                response.setStatus(metadataNode.path("status").asText("WAITING"));
                response.setCurrentQuestionNumber(metadataNode.path("currentQuestionIndex").asInt(0) + 1);
                response.setParticipantCount(metadataNode.path("participantCount").asInt(1));
            } else {
                response.setStatus("WAITING");
                response.setCurrentQuestionNumber(1);
                response.setParticipantCount(1);
            }
        } catch (Exception e) {
            log.warn("세션 상태 파싱 실패, 기본값 사용 - sessionId: {}", sessionId);
            response.setStatus("WAITING");
            response.setCurrentQuestionNumber(1);
            response.setParticipantCount(1);
        }

        response.setStartedAt(session.getCreatedAt());

        // 기본값 설정
        if (response.getTotalQuestions() == 0) {
            response.setTotalQuestions(10);
        }
        if (response.getSubjectTitle() == null) {
            response.setSubjectTitle("일반");
        }

        return response;
    }

    /**
     * 세션 활성 여부 확인
     */
    public boolean isSessionActive(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            return false;
        }

        // 세션에 연결된 문제가 있는지 확인
        Optional<Problem> problemOpt = problemRepository.findByChatSessionUuid(sessionId);
        if (!problemOpt.isPresent()) {
            return false;
        }

        ChatSession session = sessionOpt.get();

        // 세션 상태 확인
        if (session.getStatus() != ChatSession.SessionStatus.ACTIVE) {
            return false;
        }

        String metadataJson = session.getExtractedKeywords();

        try {
            if (metadataJson != null && !metadataJson.isEmpty()) {
                JsonNode metadataNode = objectMapper.readTree(metadataJson);
                String status = metadataNode.path("status").asText("WAITING");
                return "IN_PROGRESS".equals(status) || "WAITING".equals(status);
            }
            // metadata가 없으면 WAITING 상태로 간주
            return true;
        } catch (Exception e) {
            log.warn("세션 상태 확인 실패 - sessionId: {}", sessionId);
            return true; // 파싱 실패 시 활성으로 간주
        }
    }

    /**
     * 참가자 여부 확인
     */
    public boolean isParticipant(String sessionId, String userId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            return false;
        }

        ChatSession session = sessionOpt.get();
        // User 관계를 통해 세션 소유자 확인
        if (session.getUser() != null) {
            return session.getUser().getUuid().equals(userId);
        }

        // User 관계가 없으면 모든 사용자 허용
        return true;
    }

    /**
     * 세션 메타데이터 업데이트
     */
    public void updateSessionMetadata(String sessionId, Map<String, Object> updates) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new RuntimeException("세션을 찾을 수 없습니다.");
        }

        ChatSession session = sessionOpt.get();
        String metadataJson = session.getExtractedKeywords();

        try {
            Map<String, Object> existingData = new HashMap<>();

            if (metadataJson != null && !metadataJson.isEmpty()) {
                JsonNode metadataNode = objectMapper.readTree(metadataJson);
                existingData = objectMapper.convertValue(metadataNode, Map.class);
            }

            // 업데이트할 데이터 병합
            existingData.putAll(updates);

            // 다시 JSON으로 변환하여 저장
            String updatedContent = objectMapper.writeValueAsString(existingData);
            session.setExtractedKeywords(updatedContent);
            session.setUpdatedAt(LocalDateTime.now());

            chatSessionRepository.save(session);

            log.info("세션 메타데이터 업데이트 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("세션 메타데이터 업데이트 실패 - sessionId: {}", sessionId, e);
            throw new RuntimeException("세션 메타데이터 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 다음 문제로 이동
     */
    public Map<String, Object> moveToNextQuestion(String sessionId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                throw new RuntimeException("세션을 찾을 수 없습니다.");
            }

            ChatSession session = sessionOpt.get();
            int currentIndex = getCurrentQuestionIndex(session);

            // 현재 인덱스를 1 증가
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentQuestionIndex", currentIndex + 1);
            updates.put("status", "IN_PROGRESS");

            updateSessionMetadata(sessionId, updates);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("currentQuestionNumber", currentIndex + 2);
            result.put("message", "다음 문제로 이동했습니다.");

            return result;
        } catch (Exception e) {
            log.error("다음 문제 이동 실패 - sessionId: {}", sessionId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "다음 문제로 이동에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }
}