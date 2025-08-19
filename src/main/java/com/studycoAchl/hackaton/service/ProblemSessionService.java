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
     * 문제풀이 세션 생성
     */
    public Map<String, Object> createProblemSession(String title, String userUuid,
                                                    String subjectUuid, int questionCount,
                                                    String difficulty, String category) {
        try {
            log.info("문제풀이 세션 생성 시작 - title: {}, questionCount: {}", title, questionCount);

            // 1. 새로운 채팅 세션 생성
            String sessionId = UUID.randomUUID().toString();
            ChatSession chatSession = new ChatSession();
            chatSession.setUuid(sessionId);
            chatSession.setChatTitle(title);
            // userUuid와 subjectUuid는 엔티티 관계를 통해 설정해야 함
            chatSession.setCreatedData(LocalDateTime.now());

            // 2. 세션 메타데이터 설정
            Map<String, Object> sessionMetadata = new HashMap<>();
            sessionMetadata.put("status", "WAITING");
            sessionMetadata.put("currentQuestionIndex", 0);
            sessionMetadata.put("totalQuestions", questionCount);
            sessionMetadata.put("participantCount", 1);
            sessionMetadata.put("difficulty", difficulty);
            sessionMetadata.put("category", category);

            // JSON으로 변환하여 messages 필드에 저장
            String metadataJson = objectMapper.writeValueAsString(sessionMetadata);
            chatSession.setMessages(metadataJson);

            // 3. 데이터베이스에 저장
            ChatSession savedSession = chatSessionRepository.save(chatSession);

            // 4. 임시 문제 데이터 생성
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
     * 임시 문제 데이터 생성
     */
    private Problem createMockProblem(String sessionId, String userUuid, String subjectUuid, int questionCount) {
        try {
            List<Map<String, Object>> questions = new ArrayList<>();

            // 샘플 문제들 생성
            for (int i = 0; i < questionCount; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i + 1);
                question.put("question", "문제 " + (i + 1) + ": 다음 중 올바른 답은?");
                question.put("options", Arrays.asList("선택지 1", "선택지 2", "선택지 3", "선택지 4", "선택지 5"));
                question.put("correctAnswer", 0);
                question.put("difficulty", "보통");
                question.put("timeLimit", 30);
                question.put("hint", "힌트: 첫 번째 선택지를 고려해보세요.");
                questions.add(question);
            }

            Map<String, Object> problemData = new HashMap<>();
            problemData.put("questions", questions);
            problemData.put("totalCount", questionCount);
            problemData.put("createdAt", LocalDateTime.now().toString());

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
     * 현재 문제 인덱스 조회
     */
    private int getCurrentQuestionIndex(ChatSession session) {
        String messagesJson = session.getMessages();

        if (messagesJson == null || messagesJson.isEmpty()) {
            return 0; // 기본값: 첫 번째 문제
        }

        try {
            JsonNode messagesNode = objectMapper.readTree(messagesJson);
            return messagesNode.path("currentQuestionIndex").asInt(0);
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

        // 메시지에서 현재 상태 파싱
        try {
            String messagesJson = session.getMessages();
            if (messagesJson != null && !messagesJson.isEmpty()) {
                JsonNode messagesNode = objectMapper.readTree(messagesJson);

                response.setStatus(messagesNode.path("status").asText("WAITING"));
                response.setCurrentQuestionNumber(messagesNode.path("currentQuestionIndex").asInt(0) + 1);
                response.setParticipantCount(messagesNode.path("participantCount").asInt(1));
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

        response.setStartedAt(session.getCreatedData());

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
        String messagesJson = session.getMessages();

        try {
            if (messagesJson != null && !messagesJson.isEmpty()) {
                JsonNode messagesNode = objectMapper.readTree(messagesJson);
                String status = messagesNode.path("status").asText("WAITING");
                return "IN_PROGRESS".equals(status) || "WAITING".equals(status);
            }
            // messages가 없으면 WAITING 상태로 간주
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
        // 현재 구조상 별도의 참가자 테이블이 없으므로
        // 세션 생성자인지 확인하거나 기본적으로 허용
        Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            return false;
        }

        ChatSession session = sessionOpt.get();
        // 세션 생성자이거나 모든 사용자 허용 (현재는 모든 사용자 허용)
        return true;
    }
}