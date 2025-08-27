package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.entity.Record;
import com.studycoAchl.hackaton.entity.Problem;
import com.studycoAchl.hackaton.service.RecordService;
import com.studycoAchl.hackaton.service.ProblemGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
public class RecordController {

    private final RecordService recordService;
    private final ProblemGenerationService problemGenerationService;

    /**
     * 음성 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Record>> uploadRecord(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("title") String title) {

        try {
            Record record = recordService.uploadAudioFile(userId, title, file);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(record, "음성 파일이 업로드되었습니다."));

        } catch (Exception e) {
            log.error("음성 파일 업로드 실패", e);
            return ResponseEntity.ok(ApiResponse.error("파일 업로드 실패: " + e.getMessage()));
        }
    }

    /**
     * STT 변환 실행
     */
    @PostMapping("/{recordId}/transcribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transcribeRecord(@PathVariable UUID recordId) {
        try {
            Map<String, Object> result = recordService.transcribeRecord(recordId);
            return ResponseEntity.ok(ApiResponse.success(result, "음성을 텍스트로 변환했습니다."));

        } catch (Exception e) {
            log.error("STT 변환 실패 - recordId: {}", recordId, e);
            return ResponseEntity.ok(ApiResponse.error("STT 변환 실패: " + e.getMessage()));
        }
    }

    /**
     * 키워드 추출
     */
    @PostMapping("/{recordId}/extract-keywords")
    public ResponseEntity<ApiResponse<Map<String, Object>>> extractKeywords(
            @PathVariable UUID recordId,
            @RequestParam(required = false) UUID subjectId) {

        try {
            Map<String, Object> result = recordService.extractKeywordsFromRecord(recordId, subjectId);
            return ResponseEntity.ok(ApiResponse.success(result, "키워드를 추출했습니다."));

        } catch (Exception e) {
            log.error("키워드 추출 실패 - recordId: {}", recordId, e);
            return ResponseEntity.ok(ApiResponse.error("키워드 추출 실패: " + e.getMessage()));
        }
    }

    /**
     * 녹음 기반 문제 생성
     */
    @PostMapping("/{recordId}/generate-problems")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateProblems(
            @PathVariable UUID recordId,
            @RequestParam UUID userId,
            @RequestParam UUID subjectId,
            @RequestParam(defaultValue = "5") int questionCount) {

        try {
            // 키워드 추출
            Map<String, Object> keywordResult = recordService.extractKeywordsFromRecord(recordId, subjectId);

            if (!(Boolean) keywordResult.get("success")) {
                return ResponseEntity.ok(ApiResponse.error("키워드 추출에 실패했습니다."));
            }

            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) keywordResult.get("keywords");

            if (keywords.size() < 3) {
                return ResponseEntity.ok(ApiResponse.error("문제 생성을 위해서는 최소 3개 이상의 키워드가 필요합니다."));
            }

            // 문제 생성
            Map<String, Object> problemResult = problemGenerationService.generateProblemsFromKeywords(
                    userId, subjectId, keywords, "녹음 내용을 바탕으로 한 문제", questionCount);

            return ResponseEntity.ok(ApiResponse.success(problemResult, "녹음 내용을 바탕으로 문제가 생성되었습니다."));

        } catch (Exception e) {
            log.error("문제 생성 실패 - recordId: {}", recordId, e);
            return ResponseEntity.ok(ApiResponse.error("문제 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 사용자의 녹음 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<Record>>> getUserRecords(@PathVariable UUID userId) {
        try {
            List<Record> records = recordService.getUserRecords(userId);
            return ResponseEntity.ok(ApiResponse.success(records, "녹음 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("녹음 목록 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("녹음 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 녹음 삭제
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<String>> deleteRecord(@PathVariable UUID recordId) {
        try {
            recordService.deleteRecord(recordId);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "녹음이 삭제되었습니다."));

        } catch (Exception e) {
            log.error("녹음 삭제 실패 - recordId: {}", recordId, e);
            return ResponseEntity.ok(ApiResponse.error("녹음 삭제 실패: " + e.getMessage()));
        }
    }
}
