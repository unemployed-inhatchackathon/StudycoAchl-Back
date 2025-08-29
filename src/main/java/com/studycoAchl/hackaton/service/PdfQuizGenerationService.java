package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.StudyMaterial;
import com.studycoAchl.hackaton.entity.MaterialQuiz;
import com.studycoAchl.hackaton.repository.StudyMaterialRepository;
import com.studycoAchl.hackaton.repository.MaterialQuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PdfQuizGenerationService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final MaterialQuizRepository materialQuizRepository;
    private final QuestionGenerationService questionGenerationService;
    private final MaterialSummaryService materialSummaryService;

    /**
     * PDF 요약 기반 문제 생성
     */
    public Map<String, Object> generateQuizFromSummary(UUID materialUuid, int questionCount) {
        try {
            log.info("PDF 요약 기반 문제 생성 시작 - materialUuid: {}, count: {}", materialUuid, questionCount);

            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            if (!material.hasSummary()) {
                return Map.of(
                        "success", false,
                        "error", "아직 요약이 생성되지 않았습니다. 요약 완료 후 다시 시도해주세요."
                );
            }

            // 요약에서 키워드 추출
            String keywords = materialSummaryService.extractKeywords(
                    material.getAiSummary(),
                    material.getSubject().getTitle()
            );

            // AI 문제 생성
            String context = "PDF 교안 요약을 바탕으로 한 문제: " + material.getFileName();
            List<String> keywordList = Arrays.asList(keywords.split(",")).stream()
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();

            String quizJson = questionGenerationService.generateQuestionsJson(keywordList, context, questionCount);

            // MaterialQuiz 저장
            MaterialQuiz quiz = MaterialQuiz.createFromSummary(material, keywords, quizJson, questionCount);
            MaterialQuiz savedQuiz = materialQuizRepository.save(quiz);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("quizUuid", savedQuiz.getUuid());
            result.put("materialUuid", materialUuid);
            result.put("questionCount", questionCount);
            result.put("keywords", keywords);
            result.put("generationMethod", "AI_SUMMARY");
            result.put("message", "PDF 요약을 바탕으로 " + questionCount + "개의 문제가 생성되었습니다!");

            return result;

        } catch (Exception e) {
            log.error("PDF 요약 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * PDF 전체 텍스트 기반 문제 생성
     */
    public Map<String, Object> generateQuizFromFullText(UUID materialUuid, int questionCount, String difficulty) {
        try {
            log.info("PDF 전체 텍스트 기반 문제 생성 - materialUuid: {}", materialUuid);

            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            if (material.getExtractedText() == null || material.getExtractedText().isEmpty()) {
                return Map.of(
                        "success", false,
                        "error", "PDF 텍스트 추출이 완료되지 않았습니다."
                );
            }

            // 전체 텍스트에서 직접 키워드 추출
            String keywords = materialSummaryService.extractKeywords(
                    material.getExtractedText(),
                    material.getSubject().getTitle()
            );

            // AI 문제 생성
            String context = String.format("PDF 교안 '%s'의 전체 내용을 바탕으로 한 %s 난이도 문제",
                    material.getFileName(), difficulty);

            List<String> keywordList = Arrays.asList(keywords.split(",")).stream()
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();

            String quizJson = questionGenerationService.generateQuestionsJson(keywordList, context, questionCount);

            // MaterialQuiz 저장
            MaterialQuiz quiz = MaterialQuiz.createFromKeywords(material, keywords, quizJson, questionCount, difficulty);
            quiz.setGenerationMethod("FULL_TEXT");
            MaterialQuiz savedQuiz = materialQuizRepository.save(quiz);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("quizUuid", savedQuiz.getUuid());
            result.put("materialUuid", materialUuid);
            result.put("questionCount", questionCount);
            result.put("difficulty", difficulty);
            result.put("keywords", keywords);
            result.put("generationMethod", "FULL_TEXT");
            result.put("message", "PDF 전체 내용을 바탕으로 " + difficulty + " 난이도의 " + questionCount + "개 문제가 생성되었습니다!");

            return result;

        } catch (Exception e) {
            log.error("PDF 전체 텍스트 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 키워드 직접 입력으로 문제 생성
     */
    public Map<String, Object> generateQuizFromKeywords(UUID materialUuid, String keywords,
                                                        int questionCount, String difficulty) {
        try {
            log.info("키워드 기반 문제 생성 - materialUuid: {}, keywords: {}", materialUuid, keywords);

            StudyMaterial material = studyMaterialRepository.findById(materialUuid)
                    .orElseThrow(() -> new RuntimeException("학습자료를 찾을 수 없습니다."));

            // 키워드 리스트 생성
            List<String> keywordList = Arrays.asList(keywords.split(",")).stream()
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .toList();

            if (keywordList.isEmpty()) {
                return Map.of(
                        "success", false,
                        "error", "유효한 키워드가 없습니다."
                );
            }

            // AI 문제 생성
            String context = String.format("'%s' 교안의 특정 키워드들을 중심으로 한 %s 난이도 문제",
                    material.getFileName(), difficulty);

            String quizJson = questionGenerationService.generateQuestionsJson(keywordList, context, questionCount);

            // MaterialQuiz 저장
            MaterialQuiz quiz = MaterialQuiz.createFromKeywords(material, keywords, quizJson, questionCount, difficulty);
            quiz.setGenerationMethod("KEYWORD_BASED");
            MaterialQuiz savedQuiz = materialQuizRepository.save(quiz);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("quizUuid", savedQuiz.getUuid());
            result.put("materialUuid", materialUuid);
            result.put("questionCount", questionCount);
            result.put("difficulty", difficulty);
            result.put("keywords", keywords);
            result.put("generationMethod", "KEYWORD_BASED");
            result.put("message", "입력한 키워드를 바탕으로 " + difficulty + " 난이도의 " + questionCount + "개 문제가 생성되었습니다!");

            return result;

        } catch (Exception e) {
            log.error("키워드 기반 문제 생성 실패 - materialUuid: {}", materialUuid, e);
            return Map.of(
                    "success", false,
                    "error", "문제 생성 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 생성된 퀴즈 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuizDetail(UUID quizUuid) {
        try {
            MaterialQuiz quiz = materialQuizRepository.findById(quizUuid)
                    .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));

            Map<String, Object> detail = new HashMap<>();
            detail.put("quizUuid", quiz.getUuid());
            detail.put("materialUuid", quiz.getStudyMaterial().getUuid());
            detail.put("materialFileName", quiz.getStudyMaterial().getFileName());
            detail.put("subjectTitle", quiz.getSubject().getTitle());
            detail.put("questionCount", quiz.getQuestionCount());
            detail.put("difficulty", quiz.getDifficulty());
            detail.put("keywords", quiz.getKeywords());
            detail.put("generationMethod", quiz.getGenerationMethod());
            detail.put("createdAt", quiz.getCreatedAt());
            detail.put("quizData", quiz.getQuizData());

            return detail;

        } catch (Exception e) {
            log.error("퀴즈 상세 조회 실패 - quizUuid: {}", quizUuid, e);
            throw new RuntimeException("퀴즈 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 학습자료별 퀴즈 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMaterialQuizzes(UUID materialUuid) {
        try {
            List<MaterialQuiz> quizzes = materialQuizRepository.findByStudyMaterial_Uuid(materialUuid);

            return quizzes.stream()
                    .map(this::toQuizSummaryMap)
                    .toList();

        } catch (Exception e) {
            log.error("학습자료별 퀴즈 조회 실패 - materialUuid: {}", materialUuid, e);
            throw new RuntimeException("퀴즈 목록 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자별 PDF 퀴즈 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserPdfQuizzes(UUID userUuid) {
        try {
            List<MaterialQuiz> quizzes = materialQuizRepository.findByAppUsers_UuidOrderByCreatedAtDesc(userUuid);

            return quizzes.stream()
                    .map(this::toQuizSummaryMap)
                    .toList();

        } catch (Exception e) {
            log.error("사용자별 PDF 퀴즈 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("PDF 퀴즈 목록 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 퀴즈 삭제
     */
    public void deleteQuiz(UUID quizUuid) {
        try {
            MaterialQuiz quiz = materialQuizRepository.findById(quizUuid)
                    .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));

            materialQuizRepository.delete(quiz);
            log.info("퀴즈 삭제 완료 - quizUuid: {}", quizUuid);

        } catch (Exception e) {
            log.error("퀴즈 삭제 실패 - quizUuid: {}", quizUuid, e);
            throw new RuntimeException("퀴즈 삭제 실패: " + e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * MaterialQuiz를 요약 맵으로 변환
     */
    private Map<String, Object> toQuizSummaryMap(MaterialQuiz quiz) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("quizUuid", quiz.getUuid());
        summary.put("materialFileName", quiz.getStudyMaterial().getFileName());
        summary.put("questionCount", quiz.getQuestionCount());
        summary.put("difficulty", quiz.getDifficulty());
        summary.put("generationMethod", quiz.getGenerationMethod());
        summary.put("keywords", quiz.getKeywords());
        summary.put("createdAt", quiz.getCreatedAt());
        summary.put("subjectTitle", quiz.getSubject().getTitle());
        return summary;
    }

    /**
     * PDF 퀴즈 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPdfQuizStats(UUID userUuid) {
        try {
            List<MaterialQuiz> allQuizzes = materialQuizRepository.findByAppUsers_Uuid(userUuid);

            long totalQuizzes = allQuizzes.size();
            Map<String, Long> methodCounts = allQuizzes.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            MaterialQuiz::getGenerationMethod,
                            java.util.stream.Collectors.counting()
                    ));

            Map<String, Long> difficultyCounts = allQuizzes.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            MaterialQuiz::getDifficulty,
                            java.util.stream.Collectors.counting()
                    ));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPdfQuizzes", totalQuizzes);
            stats.put("generationMethodCounts", methodCounts);
            stats.put("difficultyCounts", difficultyCounts);
            stats.put("averageQuestionsPerQuiz", allQuizzes.stream()
                    .mapToInt(MaterialQuiz::getQuestionCount)
                    .average()
                    .orElse(0.0));

            return stats;

        } catch (Exception e) {
            log.error("PDF 퀴즈 통계 조회 실패 - userUuid: {}", userUuid, e);
            throw new RuntimeException("통계 조회 실패: " + e.getMessage());
        }
    }
}