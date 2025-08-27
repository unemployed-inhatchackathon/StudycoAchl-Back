package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.*;
import com.studycoAchl.hackaton.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WrongAnswerService {

    private final WrongAnswerNoteRepository wrongAnswerNoteRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 오답노트 조회
     */
    public Map<String, Object> getUserWrongAnswers(UUID userUuid) {
        try {
            log.info("오답노트 조회 - userUuid: {}", userUuid);

            List<WrongAnswerNote> allWrongNotes = wrongAnswerNoteRepository.findByUser_Uuid(userUuid);
            List<WrongAnswerNote> notMastered = wrongAnswerNoteRepository.findNotMasteredByUser(userUuid);

            Map<String, Object> result = new HashMap<>();
            result.put("totalWrongAnswers", allWrongNotes.size());
            result.put("notMasteredCount", notMastered.size());
            result.put("masteredCount", allWrongNotes.size() - notMastered.size());
            result.put("wrongAnswerNotes", allWrongNotes);

            // 키워드별 그룹화
            Map<String, List<WrongAnswerNote>> groupedByKeyword = allWrongNotes.stream()
                    .collect(Collectors.groupingBy(WrongAnswerNote::getKeyword));
            result.put("groupedByKeyword", groupedByKeyword);

            return result;

        } catch (Exception e) {
            log.error("오답노트 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("오답노트 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 과목별 오답노트 조회
     */
    public Map<String, Object> getSubjectWrongAnswers(UUID userUuid, UUID subjectUuid) {
        try {
            List<WrongAnswerNote> subjectWrongNotes = wrongAnswerNoteRepository.findByUser_UuidAndSubject_Uuid(userUuid, subjectUuid);
            List<WrongAnswerNote> notMastered = wrongAnswerNoteRepository.findNotMasteredByUserAndSubject(userUuid, subjectUuid);

            Map<String, Object> result = new HashMap<>();
            result.put("subjectWrongAnswers", subjectWrongNotes);
            result.put("notMasteredCount", notMastered.size());
            result.put("totalCount", subjectWrongNotes.size());

            return result;

        } catch (Exception e) {
            log.error("과목별 오답노트 조회 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            throw new RuntimeException("과목별 오답노트 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 복습용 문제 생성 (틀린 문제만으로)
     */
    public Map<String, Object> generateReviewQuiz(UUID userUuid, UUID subjectUuid, int maxQuestions) {
        try {
            log.info("복습 퀴즈 생성 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid);

            AppUsers appUsers = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            Subject subject = subjectRepository.findById(subjectUuid)
                    .orElseThrow(() -> new RuntimeException("과목을 찾을 수 없습니다."));

            // 복습이 필요한 오답 노트들 조회
            List<WrongAnswerNote> reviewItems = wrongAnswerNoteRepository.findNotMasteredByUserAndSubject(userUuid, subjectUuid);

            if (reviewItems.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "복습이 필요한 문제가 없습니다."
                );
            }

            // 최대 문제 수만큼 선택 (최근 틀린 순서대로)
            List<WrongAnswerNote> selectedItems = reviewItems.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(maxQuestions)
                    .collect(Collectors.toList());

            // 복습용 Problem JSON 생성
            String reviewProblemsJson = createReviewProblemsJson(selectedItems);

            // Problem 엔티티 생성
            Problem reviewProblem = Problem.builder()
                    .problems(reviewProblemsJson)
                    .appUsers(appUsers)
                    .subject(subject)
                    .chatSession(null) // 복습 문제는 채팅과 무관
                    .createdData(LocalDateTime.now())
                    .build();

            Problem savedProblem = problemRepository.save(reviewProblem);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("reviewProblemUuid", savedProblem.getUuid());
            result.put("reviewQuestionsCount", selectedItems.size());
            result.put("message", selectedItems.size() + "개의 복습 문제가 생성되었습니다.");

            return result;

        } catch (Exception e) {
            log.error("복습 퀴즈 생성 실패 - userUuid: {}, subjectUuid: {}", userUuid, subjectUuid, e);
            return Map.of(
                    "success", false,
                    "error", "복습 퀴즈 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 오답 복습 완료 처리
     */
    public Map<String, Object> markReviewCompleted(UUID wrongAnswerNoteUuid, boolean isCorrect) {
        try {
            WrongAnswerNote wrongNote = wrongAnswerNoteRepository.findById(wrongAnswerNoteUuid)
                    .orElseThrow(() -> new RuntimeException("오답노트를 찾을 수 없습니다."));

            wrongNote.markAsReviewed();

            // 연속 3번 맞히면 완전 숙지 처리
            if (isCorrect && wrongNote.getReviewCount() >= 3) {
                wrongNote.markAsMastered();
            }

            WrongAnswerNote updatedNote = wrongAnswerNoteRepository.save(wrongNote);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("reviewCount", updatedNote.getReviewCount());
            result.put("isMastered", updatedNote.getIsMastered());
            result.put("isCorrectThisTime", isCorrect);

            if (updatedNote.getIsMastered()) {
                result.put("message", "완전 숙지! 이 문제는 더 이상 복습하지 않아도 됩니다.");
            } else {
                result.put("message", isCorrect ? "정답! 복습 진도가 올라갔습니다." : "다시 도전해보세요.");
            }

            return result;

        } catch (Exception e) {
            log.error("복습 완료 처리 실패 - wrongAnswerNoteUuid: {}", wrongAnswerNoteUuid, e);
            return Map.of(
                    "success", false,
                    "error", "복습 완료 처리 실패: " + e.getMessage()
            );
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 복습용 문제 JSON 생성
     */
    private String createReviewProblemsJson(List<WrongAnswerNote> reviewItems) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("title", "오답 복습 문제");
            root.put("totalQuestions", reviewItems.size());
            root.put("keywords", reviewItems.stream().map(WrongAnswerNote::getKeyword).distinct().collect(Collectors.joining(", ")));
            root.put("createdAt", LocalDateTime.now().toString());
            root.put("source", "Wrong Answer Review");

            ArrayNode questionsArray = objectMapper.createArrayNode();

            for (int i = 0; i < reviewItems.size(); i++) {
                WrongAnswerNote item = reviewItems.get(i);

                ObjectNode question = objectMapper.createObjectNode();
                question.put("id", i + 1);
                question.put("question", item.getQuestionText());

                // 기존 선택지 파싱
                JsonNode optionsNode = objectMapper.readTree(item.getOptions());
                question.set("options", optionsNode);

                question.put("correctAnswer", item.getCorrectAnswer());
                question.put("explanation", item.getExplanation());
                question.put("keyword", item.getKeyword());
                question.put("difficulty", "복습");
                question.put("timeLimit", 60);
                question.put("hint", "이전에 틀린 문제입니다. 다시 한번 신중하게 생각해보세요.");
                question.put("wrongAnswerNoteUuid", item.getUuid().toString()); // 복습 처리용

                questionsArray.add(question);
            }

            root.set("questions", questionsArray);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            log.error("복습 문제 JSON 생성 실패", e);
            throw new RuntimeException("복습 문제 생성 실패: " + e.getMessage());
        }
    }
}