package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.dto.CurrentQuestionResponse;
import com.studycoAchl.hackaton.dto.SessionStatusResponse;
import com.studycoAchl.hackaton.entity.AppUsers;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.repository.ChatSessionRepository;
import com.studycoAchl.hackaton.repository.ProblemRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProblemSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ProblemRepository problemRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ProblemGenerationService problemGenerationService; // 추가
    private final QuestionGenerationService questionGenerationService;

    /**
     * 문제풀이 세션 생성 (키워드 기반)
     */
    public Map<String, Object> createProblemSession(String title, UUID userUuid,
                                                    UUID subjectUuid, int questionCount,
                                                    String difficulty, String category) {
        try {
            log.info("문제풀이 세션 생성 시작 - title: {}, questionCount: {}", title, questionCount);

            AppUsers appUsers = findUserWithValidation(userUuid);
            Subject subject = findSubjectWithValidation(subjectUuid);

            // ChatSession 엔티티를 생성하고, 메타데이터를 저장
            ChatSession chatSession = ChatSession.builder()
                    .chatTitle(title)
                    .appUsers(appUsers)
                    .subject(subject)
                    .createdData(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .status(ChatSession.SessionStatus.ACTIVE)
                    .generatedProblemCount(0)
                    .messages(new ArrayList<>())
                    .build();

            // 문제 풀이 메타데이터 JSON 생성
            Map<String, Object> sessionMetadata = new HashMap<>();
            sessionMetadata.put("status", "WAITING");
            sessionMetadata.put("currentQuestionIndex", 0);
            sessionMetadata.put("totalQuestions", questionCount);

            String metadataJson = objectMapper.writeValueAsString(sessionMetadata);
            chatSession.setExtractedKeywords(metadataJson);

            ChatSession savedSession = chatSessionRepository.save(chatSession);

            // AI 기반 문제 생성 (이 부분은 기존 로직을 따름)
            List<String> keywords = problemGenerationService.getKeywordsBySubject(subjectUuid);
            String problemsJson = questionGenerationService.generateQuestionsJson(keywords, title, questionCount);

            Problem problem = Problem.builder()
                    .problems(problemsJson)
                    .appUsers(appUsers)
                    .subject(subject)
                    .chatSession(savedSession) // 생성된 채팅 세션과 연결
                    .createdData(LocalDateTime.now())
                    .build();

            problemRepository.save(problem);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("problemUuid", problem.getUuid());
            result.put("sessionId", savedSession.getUuid());
            result.put("title", title);
            result.put("questionCount", questionCount);
            result.put("message", "세션이 성공적으로 생성되었습니다.");

            return result;

        } catch (Exception e) {
            log.error("문제풀이 세션 생성 실패", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "세션 생성에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    public CurrentQuestionResponse getCurrentQuestion(UUID problemUuid) {
        log.info("현재 문제 조회 시작 - problemUuid: {}", problemUuid);

        Problem problem = problemRepository.findById(problemUuid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다. ID: " + problemUuid));

        JsonNode metadataNode = null;
        ChatSession session = problem.getChatSession();
        if (session != null) {
            try {
                String metadataJson = session.getExtractedKeywords();
                if (metadataJson != null && !metadataJson.isEmpty()) {
                    metadataNode = objectMapper.readTree(metadataJson);
                }
            } catch (Exception e) {
                log.warn("세션 메타데이터 파싱 실패, 문제 조회는 계속 진행 - problemUuid: {}", problemUuid, e);
            }
        } else {
            // 채팅 세션이 없는 경우, 이 API는 사실상 사용 불가.
            // 그러나 임시방편으로 문제 자체 정보만 반환
            log.warn("문제에 연결된 채팅 세션이 없습니다. 문제 데이터만 반환합니다.");
            // 다음 문제 이동 등의 상태 관리가 불가능하므로, 첫 문제만 반환
        }

        int currentQuestionIndex = (metadataNode != null) ? metadataNode.path("currentQuestionIndex").asInt(0) : 0;

        String problemsJson = problem.getProblems();
        if (!StringUtils.hasText(problemsJson)) {
            throw new RuntimeException("문제 데이터가 없습니다.");
        }

        try {
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");
            if (questionsArray == null || !questionsArray.isArray()) {
                throw new RuntimeException("문제 배열을 찾을 수 없습니다.");
            }
            if (currentQuestionIndex >= questionsArray.size()) {
                throw new RuntimeException("모든 문제가 완료되었습니다.");
            }

            JsonNode currentQuestion = questionsArray.get(currentQuestionIndex);
            CurrentQuestionResponse response = new CurrentQuestionResponse();
            response.setQuestionId(currentQuestion.path("id").asText());
            response.setQuestionText(currentQuestion.path("question").asText());
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
            if (problem.getSubject() != null) {
                response.setCategory(problem.getSubject().getTitle());
            }

            log.info("현재 문제 조회 완료 - problemUuid: {}, questionNumber: {}",
                    problemUuid, response.getQuestionNumber());

            return response;
        } catch (Exception e) {
            log.error("문제 데이터 파싱 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("문제 데이터를 파싱할 수 없습니다: " + e.getMessage());
        }
    }

    public SessionStatusResponse getSessionStatus(UUID problemUuid) {
        log.info("세션 상태 조회 시작 - problemUuid: {}", problemUuid);

        Problem problem = problemRepository.findById(problemUuid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        JsonNode metadataNode = null;
        ChatSession session = problem.getChatSession();

        if (session != null) {
            try {
                String metadataJson = session.getExtractedKeywords();
                if (metadataJson != null && !metadataJson.isEmpty()) {
                    metadataNode = objectMapper.readTree(metadataJson);
                }
            } catch (Exception e) {
                log.warn("세션 메타데이터 파싱 실패, 기본값 사용 - problemUuid: {}", problemUuid);
            }
        } else {
            throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다.");
        }

        SessionStatusResponse response = new SessionStatusResponse();
        response.setSessionId(problemUuid.toString());

        if (metadataNode != null) {
            response.setStatus(metadataNode.path("status").asText("WAITING"));
            response.setCurrentQuestionNumber(metadataNode.path("currentQuestionIndex").asInt(0) + 1);
            response.setTotalQuestions(metadataNode.path("totalQuestions").asInt(0));
        } else {
            response.setStatus("UNKNOWN");
            response.setCurrentQuestionNumber(0);
            response.setTotalQuestions(0);
        }

        try {
            if (problem.getSubject() != null) {
                response.setSubjectTitle(problem.getSubject().getTitle());
            } else {
                response.setSubjectTitle("일반");
            }
        } catch (Exception e) {
            log.warn("과목 정보 조회 실패 - problemUuid: {}", problemUuid);
            response.setSubjectTitle("일반");
        }

        return response;
    }

    public Map<String, Object> moveToNextQuestion(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            ChatSession session = problem.getChatSession();
            if (session == null) {
                // 이 부분을 변경하여 더 명확한 메시지를 반환합니다.
                throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다. 문제풀이 세션의 상태 관리는 채팅 기반 문제에만 지원됩니다.");
            }

            String problemsJson = problem.getProblems();
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");
            int totalQuestions = questionsArray.size();

            JsonNode metadataNode = objectMapper.readTree(session.getExtractedKeywords());
            int currentIndex = metadataNode.path("currentQuestionIndex").asInt(0);

            if (currentIndex + 1 >= totalQuestions) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "COMPLETED");
                updateSessionMetadata(problemUuid, updates);

                return Map.of(
                        "problemUuid", problemUuid,
                        "currentQuestionNumber", currentIndex + 1,
                        "totalQuestions", totalQuestions,
                        "hasNext", false,
                        "isCompleted", true,
                        "message", "모든 문제가 완료되었습니다."
                );
            }

            int nextIndex = currentIndex + 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentQuestionIndex", nextIndex);
            updates.put("status", "IN_PROGRESS");
            updateSessionMetadata(problemUuid, updates);

            return Map.of(
                    "problemUuid", problemUuid,
                    "currentQuestionNumber", nextIndex + 1,
                    "totalQuestions", totalQuestions,
                    "hasNext", (nextIndex + 1) < totalQuestions,
                    "isCompleted", false,
                    "message", "다음 문제로 이동했습니다."
            );
        } catch (Exception e) {
            log.error("다음 문제 이동 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("다음 문제로 이동에 실패했습니다: " + e.getMessage());
        }
    }

    public Map<String, Object> moveToPreviousQuestion(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            ChatSession session = problem.getChatSession();
            if (session == null) {
                // 이 부분을 변경하여 더 명확한 메시지를 반환합니다.
                throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다. 문제풀이 세션의 상태 관리는 채팅 기반 문제에만 지원됩니다.");
            }
            String problemsJson = problem.getProblems();
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");
            int totalQuestions = questionsArray.size();

            JsonNode metadataNode = objectMapper.readTree(session.getExtractedKeywords());
            int currentIndex = metadataNode.path("currentQuestionIndex").asInt(0);

            if (currentIndex <= 0) {
                return Map.of(
                        "problemUuid", problemUuid,
                        "currentQuestionNumber", 1,
                        "totalQuestions", totalQuestions,
                        "hasPrevious", false,
                        "isFirstQuestion", true,
                        "message", "이미 첫 번째 문제입니다."
                );
            }

            int previousIndex = currentIndex - 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentQuestionIndex", previousIndex);
            updates.put("status", "IN_PROGRESS");
            updateSessionMetadata(problemUuid, updates);

            return Map.of(
                    "problemUuid", problemUuid,
                    "currentQuestionNumber", previousIndex + 1,
                    "totalQuestions", totalQuestions,
                    "hasPrevious", previousIndex > 0,
                    "hasNext", (previousIndex + 1) < totalQuestions,
                    "isFirstQuestion", previousIndex == 0,
                    "message", "이전 문제로 이동했습니다."
            );
        } catch (Exception e) {
            log.error("이전 문제 이동 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("이전 문제로 이동에 실패했습니다: " + e.getMessage());
        }
    }

    public Map<String, Object> completeSession(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            ChatSession session = problem.getChatSession();
            if (session == null) {
                // 이 부분을 변경하여 더 명확한 메시지를 반환합니다.
                throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다. 문제풀이 세션의 상태 관리는 채팅 기반 문제에만 지원됩니다.");
            }
            session.setStatus(ChatSession.SessionStatus.COMPLETED);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "COMPLETED");
            updateSessionMetadata(problemUuid, updates);

            return Map.of(
                    "success", true,
                    "message", "세션이 완료되었습니다.",
                    "completedAt", LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("세션 완료 처리 실패 - problemUuid: {}", problemUuid, e);
            return Map.of(
                    "success", false,
                    "message", "세션 완료 처리에 실패했습니다: " + e.getMessage()
            );
        }
    }

    public void updateSessionMetadata(UUID problemUuid, Map<String, Object> updates) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            ChatSession session = problem.getChatSession();
            if (session == null) {
                // 채팅 세션이 없는 경우, 업데이트 로직을 수행하지 않고 그냥 종료합니다.
                // 이 API는 채팅 기반 문제에만 유효합니다.
                log.warn("문제에 연결된 채팅 세션이 없어 메타데이터 업데이트를 건너뜁니다.");
                return;
            }

            Map<String, Object> existingData = safeParseMapFromJson(session.getExtractedKeywords());
            existingData.putAll(updates);

            String updatedContent = objectMapper.writeValueAsString(existingData);
            session.setExtractedKeywords(updatedContent);
            session.setUpdatedAt(LocalDateTime.now());

            chatSessionRepository.save(session);

        } catch (Exception e) {
            log.error("세션 메타데이터 업데이트 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("세션 메타데이터 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    private JsonNode safeParseSessionMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(metadataJson);
        } catch (Exception e) {
            log.warn("메타데이터 JSON 파싱 실패, 빈 객체 반환 - content: {}", metadataJson);
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, Object> safeParseMapFromJson(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(jsonString, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("맵 JSON 파싱 실패, 빈 맵 반환 - content: {}", jsonString);
            return new HashMap<>();
        }
    }

    public List<Problem> getProblemsForChatSession(UUID chatSessionUuid) {
        try {
            return problemRepository.findAllByChatSession_Uuid(chatSessionUuid);
        } catch (Exception e) {
            log.error("채팅 세션의 문제 세트 조회 실패 - chatSessionUuid: {}", chatSessionUuid, e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getProblemSummary(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            String problemsJson = problem.getProblems();
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");

            Map<String, Object> summary = new HashMap<>();
            summary.put("problemUuid", problemUuid);
            summary.put("title", problemsNode.path("title").asText("AI 생성 문제"));
            summary.put("totalQuestions", questionsArray != null ? questionsArray.size() : 0);
            summary.put("createdAt", problem.getCreatedData());

            if (problem.getSubject() != null) {
                summary.put("subjectTitle", problem.getSubject().getTitle());
            }

            return summary;

        } catch (Exception e) {
            log.error("문제 세트 요약 정보 조회 실패 - problemUuid: {}", problemUuid, e);
            return Map.of("error", "문제 정보를 불러올 수 없습니다.");
        }
    }

    public boolean isSessionActive(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) return false;

            ChatSession session = problem.getChatSession();
            if (session == null) return false;

            if (session.getStatus() != ChatSession.SessionStatus.ACTIVE) return false;

            String metadataJson = session.getExtractedKeywords();
            try {
                if (metadataJson != null && !metadataJson.isEmpty()) {
                    JsonNode metadataNode = objectMapper.readTree(metadataJson);
                    String status = metadataNode.path("status").asText("WAITING");
                    return "IN_PROGRESS".equals(status) || "WAITING".equals(status);
                }
                return true;
            } catch (Exception e) {
                return true;
            }
        } catch (Exception e) {
            log.error("세션 활성 여부 확인 실패 - problemUuid: {}", problemUuid, e);
            return false;
        }
    }

    public boolean isParticipant(UUID problemUuid, UUID userId) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) return false;

            ChatSession session = problem.getChatSession();
            if (session == null) return false;

            if (session.getAppUsers() != null) {
                return session.getAppUsers().getUuid().equals(userId);
            }

            return true;
        } catch (Exception e) {
            log.error("참가자 여부 확인 실패 - problemUuid: {}", problemUuid, e);
            return false;
        }
    }

    private AppUsers findUserWithValidation(UUID userUuid) {
        if (userUuid == null) {
            throw new RuntimeException("사용자 UUID가 null입니다.");
        }
        return userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userUuid));
    }

    private Subject findSubjectWithValidation(UUID subjectUuid) {
        if (subjectUuid == null) {
            throw new RuntimeException("과목 UUID가 null입니다.");
        }
        return subjectRepository.findById(subjectUuid)
                .orElseThrow(() -> new RuntimeException("과목을 찾을 수 없습니다: " + subjectUuid));
    }

    public UUID createTestUserIfNotExists(String testEmail) {
        try {
            Optional<AppUsers> existingUser = userRepository.findByEmail(testEmail);
            if (existingUser.isPresent()) {
                return existingUser.get().getUuid();
            }

            AppUsers testAppUsers = AppUsers.builder()
                    .email(testEmail)
                    .password("test123")
                    .nickname("테스트사용자")
                    .token("test_token_" + UUID.randomUUID())
                    .createdAt(LocalDateTime.now())
                    .build();

            AppUsers savedAppUsers = userRepository.save(testAppUsers);
            return savedAppUsers.getUuid();

        } catch (Exception e) {
            log.error("테스트 사용자 생성 실패", e);
            throw new RuntimeException("테스트 사용자 생성에 실패했습니다: " + e.getMessage());
        }
    }
}