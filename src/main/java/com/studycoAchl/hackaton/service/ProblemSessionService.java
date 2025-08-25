package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.dto.CurrentQuestionResponse;
import com.studycoAchl.hackaton.dto.SessionStatusResponse;
import com.studycoAchl.hackaton.entity.ChatSession;
import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.entity.User;
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

    /**
     * 문제풀이 세션 생성 (키워드 기반)
     */
    public Map<String, Object> createProblemSession(String title, UUID userUuid,
                                                    UUID subjectUuid, int questionCount,
                                                    String difficulty, String category) {
        try {
            log.info("문제풀이 세션 생성 시작 - title: {}, questionCount: {}", title, questionCount);

            // 1. 사용자와 과목 조회
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userUuid));

            Subject subject = subjectRepository.findById(subjectUuid)
                    .orElseThrow(() -> new RuntimeException("과목을 찾을 수 없습니다: " + subjectUuid));

            // 2. 새로운 채팅 세션 생성
            ChatSession chatSession = ChatSession.builder()
                    .title(title)
                    .user(user)
                    .subject(subject)
                    .createdData(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .status(ChatSession.SessionStatus.ACTIVE)
                    .generatedProblemCount(0)
                    .messages(new ArrayList<>()) // 빈 리스트로 초기화
                    .build();

            // 3. 세션 메타데이터를 extractedKeywords 필드에 임시 저장
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

            // 4. 데이터베이스에 저장
            ChatSession savedSession = chatSessionRepository.save(chatSession);

            // 5. 임시 문제 데이터 생성 (기존 DB 구조에 맞춰서)
            Problem problem = createMockProblem(savedSession, user, subject, questionCount);
            problemRepository.save(problem);

            // 6. 응답 생성
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sessionId", savedSession.getUuid());
            result.put("title", title);
            result.put("questionCount", questionCount);
            result.put("difficulty", difficulty);
            result.put("category", category);
            result.put("message", "세션이 성공적으로 생성되었습니다.");

            log.info("문제풀이 세션 생성 완료 - sessionId: {}", savedSession.getUuid());
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
    private Problem createMockProblem(ChatSession chatSession, User user, Subject subject, int questionCount) {
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

            // Problem 엔티티 생성 (Builder 패턴 사용)
            Problem problem = Problem.builder()
                    .problems(problemsJson)
                    .user(user)
                    .subject(subject)
                    .chatSession(chatSession)
                    .createdData(LocalDateTime.now())
                    .build();

            return problem;

        } catch (Exception e) {
            log.error("임시 문제 생성 실패", e);
            throw new RuntimeException("문제 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 문제 조회 - problemUuid 기반으로 변경
     */
    public CurrentQuestionResponse getCurrentQuestion(UUID problemUuid) {
        log.info("현재 문제 조회 시작 - problemUuid: {}", problemUuid);

        // 1. problemUuid로 직접 Problem 조회
        Problem problem = problemRepository.findById(problemUuid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다. ID: " + problemUuid));

        // 2. Problem과 연결된 ChatSession 조회
        ChatSession session = problem.getChatSession();
        if (session == null) {
            throw new RuntimeException("문제에 연결된 채팅 세션을 찾을 수 없습니다.");
        }

        String problemsJson = problem.getProblems();
        if (problemsJson == null || problemsJson.isEmpty()) {
            throw new RuntimeException("문제 데이터가 없습니다.");
        }

        try {
            // 3. JSON에서 문제 데이터 파싱
            JsonNode problemsNode = objectMapper.readTree(problemsJson);

            // 현재 문제 인덱스 가져오기 (ChatSession의 메타데이터에서)
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

            // 과목 정보 설정 (Subject 관계를 통해)
            if (problem.getSubject() != null) {
                response.setCategory(problem.getSubject().getTitle());
            }

            // 시간 제한과 힌트 정보
            response.setTimeLimit(currentQuestion.path("timeLimit").asInt(30));
            response.setQuestionStartTime(LocalDateTime.now());
            response.setHasHint(currentQuestion.has("hint") &&
                    !currentQuestion.path("hint").asText().isEmpty());

            log.info("현재 문제 조회 완료 - problemUuid: {}, questionNumber: {}",
                    problemUuid, response.getQuestionNumber());

            return response;

        } catch (Exception e) {
            log.error("문제 데이터 파싱 실패 - problemUuid: {}", problemUuid, e);
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
     * 세션 상태 조회 - problemUuid 기반으로 변경
     */
    public SessionStatusResponse getSessionStatus(UUID problemUuid) {
        log.info("세션 상태 조회 시작 - problemUuid: {}", problemUuid);

        // problemUuid로 Problem 조회
        Problem problem = problemRepository.findById(problemUuid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        // Problem과 연결된 ChatSession 조회
        ChatSession session = problem.getChatSession();
        if (session == null) {
            throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다.");
        }

        SessionStatusResponse response = new SessionStatusResponse();
        response.setSessionId(session.getUuid().toString());

        // 문제 정보 조회
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
            if (problem.getSubject() != null) {
                response.setSubjectTitle(problem.getSubject().getTitle());
            }

        } catch (Exception e) {
            log.warn("문제 정보 파싱 실패 - problemUuid: {}", problemUuid);
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
            log.warn("세션 상태 파싱 실패, 기본값 사용 - problemUuid: {}", problemUuid);
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
     * 세션 활성 여부 확인 - problemUuid 기반으로 변경
     */
    public boolean isSessionActive(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) {
                return false;
            }

            ChatSession session = problem.getChatSession();
            if (session == null) {
                return false;
            }

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
                log.warn("세션 상태 확인 실패 - problemUuid: {}", problemUuid);
                return true; // 파싱 실패 시 활성으로 간주
            }
        } catch (Exception e) {
            log.error("세션 활성 여부 확인 실패 - problemUuid: {}", problemUuid, e);
            return false;
        }
    }

    /**
     * 참가자 여부 확인 - problemUuid 기반으로 변경
     */
    public boolean isParticipant(UUID problemUuid, UUID userId) {
        try {
            Problem problem = problemRepository.findById(problemUuid).orElse(null);
            if (problem == null) {
                return false;
            }

            ChatSession session = problem.getChatSession();
            if (session == null) {
                return false;
            }

            // User 관계를 통해 세션 소유자 확인
            if (session.getUser() != null) {
                return session.getUser().getUuid().equals(userId);
            }

            // User 관계가 없으면 모든 사용자 허용
            return true;
        } catch (Exception e) {
            log.error("참가자 여부 확인 실패 - problemUuid: {}", problemUuid, e);
            return false;
        }
    }

    /**
     * 세션 메타데이터 업데이트 - problemUuid 기반으로 변경
     */
    // 1. ProblemSessionService.java - updateSessionMetadata 메소드
    public void updateSessionMetadata(UUID problemUuid, Map<String, Object> updates) {
        Problem problem = problemRepository.findById(problemUuid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        ChatSession session = problem.getChatSession();
        if (session == null) {
            throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다.");
        }

        String metadataJson = session.getExtractedKeywords();

        try {
            @SuppressWarnings("unchecked")  // 제네릭 타입 경고 해결
            Map<String, Object> existingData = new HashMap<>();

            if (metadataJson != null && !metadataJson.isEmpty()) {
                JsonNode metadataNode = objectMapper.readTree(metadataJson);
                existingData = objectMapper.convertValue(metadataNode,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
            }

            // 업데이트할 데이터 병합
            existingData.putAll(updates);

            // 다시 JSON으로 변환하여 저장
            String updatedContent = objectMapper.writeValueAsString(existingData);
            session.setExtractedKeywords(updatedContent);
            session.setUpdatedAt(LocalDateTime.now());

            chatSessionRepository.save(session);

            log.info("세션 메타데이터 업데이트 완료 - problemUuid: {}", problemUuid);
        } catch (Exception e) {
            log.error("세션 메타데이터 업데이트 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("세션 메타데이터 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 다음 문제로 이동 - problemUuid 기반 (이미 구현되어 있음)
     */
    public Map<String, Object> moveToNextQuestion(UUID problemUuid) {
        try {
            log.info("다음 문제로 이동 시작 - problemUuid: {}", problemUuid);

            // 1. Problem 직접 조회
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            // 2. Problem과 연결된 ChatSession 조회
            ChatSession session = problem.getChatSession();
            if (session == null) {
                throw new RuntimeException("문제에 연결된 채팅 세션을 찾을 수 없습니다.");
            }

            String problemsJson = problem.getProblems();
            if (problemsJson == null || problemsJson.isEmpty()) {
                throw new RuntimeException("문제 데이터가 비어있습니다.");
            }

            // 3. 현재 문제 인덱스 가져오기 (ChatSession의 메타데이터에서)
            int currentIndex = getCurrentQuestionIndex(session);
            log.debug("현재 문제 인덱스: {}", currentIndex);

            // 4. JSON에서 전체 문제 수 확인
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");

            if (questionsArray == null || !questionsArray.isArray()) {
                throw new RuntimeException("문제 배열을 찾을 수 없습니다.");
            }

            int totalQuestions = questionsArray.size();
            log.debug("전체 문제 수: {}, 현재 인덱스: {}", totalQuestions, currentIndex);

            // 5. 마지막 문제인지 확인
            if (currentIndex + 1 >= totalQuestions) {
                log.info("마지막 문제에 도달 - problemUuid: {}", problemUuid);

                // 세션 완료 처리
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "COMPLETED");
                updates.put("completedAt", LocalDateTime.now().toString());
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

            // 6. 다음 문제로 이동
            int nextIndex = currentIndex + 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentQuestionIndex", nextIndex);
            updates.put("status", "IN_PROGRESS");

            updateSessionMetadata(problemUuid, updates);

            log.info("다음 문제로 이동 완료 - problemUuid: {}, 다음 문제: {}", problemUuid, nextIndex + 1);

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

    /**
     * 이전 문제로 이동 - problemUuid 기반
     */
    public Map<String, Object> moveToPreviousQuestion(UUID problemUuid) {
        try {
            log.info("이전 문제로 이동 시작 - problemUuid: {}", problemUuid);

            // 1. Problem 직접 조회
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            // 2. Problem과 연결된 ChatSession 조회
            ChatSession session = problem.getChatSession();
            if (session == null) {
                throw new RuntimeException("문제에 연결된 채팅 세션을 찾을 수 없습니다.");
            }

            String problemsJson = problem.getProblems();
            if (problemsJson == null || problemsJson.isEmpty()) {
                throw new RuntimeException("문제 데이터가 비어있습니다.");
            }

            // 3. 현재 문제 인덱스 가져오기
            int currentIndex = getCurrentQuestionIndex(session);
            log.debug("현재 문제 인덱스: {}", currentIndex);

            // 4. JSON에서 전체 문제 수 확인
            JsonNode problemsNode = objectMapper.readTree(problemsJson);
            JsonNode questionsArray = problemsNode.get("questions");

            if (questionsArray == null || !questionsArray.isArray()) {
                throw new RuntimeException("문제 배열을 찾을 수 없습니다.");
            }

            int totalQuestions = questionsArray.size();
            log.debug("전체 문제 수: {}, 현재 인덱스: {}", totalQuestions, currentIndex);

            // 5. 첫 번째 문제인지 확인
            if (currentIndex <= 0) {
                log.info("첫 번째 문제에 도달 - problemUuid: {}", problemUuid);

                return Map.of(
                        "problemUuid", problemUuid,
                        "currentQuestionNumber", 1,
                        "totalQuestions", totalQuestions,
                        "hasPrevious", false,
                        "isFirstQuestion", true,
                        "message", "이미 첫 번째 문제입니다."
                );
            }

            // 6. 이전 문제로 이동
            int previousIndex = currentIndex - 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentQuestionIndex", previousIndex);
            updates.put("status", "IN_PROGRESS");

            updateSessionMetadata(problemUuid, updates);

            log.info("이전 문제로 이동 완료 - problemUuid: {}, 이전 문제: {}", problemUuid, previousIndex + 1);

            return Map.of(
                    "problemUuid", problemUuid,
                    "currentQuestionNumber", previousIndex + 1,
                    "totalQuestions", totalQuestions,
                    "hasPrevious", previousIndex > 0,
                    "hasNext", (previousIndex + 1) < totalQuestions,
                    "isFirstQuestion", previousIndex == 0,
                    "isCompleted", false,
                    "message", "이전 문제로 이동했습니다."
            );

        } catch (Exception e) {
            log.error("이전 문제 이동 실패 - problemUuid: {}", problemUuid, e);
            throw new RuntimeException("이전 문제로 이동에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 세션 완료 처리 - problemUuid 기반으로 변경
     */
    public Map<String, Object> completeSession(UUID problemUuid) {
        try {
            Problem problem = problemRepository.findById(problemUuid)
                    .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

            ChatSession session = problem.getChatSession();
            if (session == null) {
                throw new RuntimeException("문제에 연결된 세션을 찾을 수 없습니다.");
            }

            // 세션 상태를 완료로 변경
            session.setStatus(ChatSession.SessionStatus.COMPLETED);

            // 메타데이터도 업데이트
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "COMPLETED");
            updates.put("completedAt", LocalDateTime.now().toString());

            updateSessionMetadata(problemUuid, updates);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "세션이 완료되었습니다.");
            result.put("completedAt", LocalDateTime.now());

            return result;
        } catch (Exception e) {
            log.error("세션 완료 처리 실패 - problemUuid: {}", problemUuid, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "세션 완료 처리에 실패했습니다: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 채팅 세션의 모든 문제 세트 조회 (새로 추가)
     */
    public List<Problem> getProblemsForChatSession(UUID chatSessionUuid) {
        try {
            log.info("채팅 세션의 문제 세트 조회 - chatSessionUuid: {}", chatSessionUuid);
            return problemRepository.findAllByChatSession_Uuid(chatSessionUuid);
        } catch (Exception e) {
            log.error("채팅 세션의 문제 세트 조회 실패 - chatSessionUuid: {}", chatSessionUuid, e);
            return new ArrayList<>();
        }
    }

    /**
     * 문제 세트 요약 정보 조회 (새로 추가)
     */
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
            summary.put("keywords", problemsNode.path("keywords").asText(""));
            summary.put("createdAt", problem.getCreatedData());

            if (problem.getSubject() != null) {
                summary.put("subjectTitle", problem.getSubject().getTitle());
            }

            return summary;

        } catch (Exception e) {
            log.error("문제 세트 요약 정보 조회 실패 - problemUuid: {}", problemUuid, e);
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "문제 정보를 불러올 수 없습니다.");
            return errorSummary;
        }
    }
}