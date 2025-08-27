package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.dto.SubjectResponseDto;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;

    /**
     * 과목 생성
     */
    @PostMapping(value = "/users/{userUuid}/subjects", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ApiResponse<SubjectResponseDto>> createSubject(
            @PathVariable UUID userUuid,
            @RequestBody String title) {

        try {
            Subject createdSubject = subjectService.createSubject(userUuid, title);
            SubjectResponseDto response = subjectService.toResponseDto(createdSubject);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "과목이 생성되었습니다."));

        } catch (Exception e) {
            log.error("과목 생성 실패 - userUuid: {}, title: {}", userUuid, title, e);
            return ResponseEntity.ok(ApiResponse.error("과목 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 과목 목록 조회
     */
    @GetMapping("/users/{userUuid}/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponseDto>>> getUserSubjects(@PathVariable UUID userUuid) {
        try {
            List<Subject> subjects = subjectService.getUserSubjects(userUuid);
            List<SubjectResponseDto> response = subjects.stream()
                    .map(subjectService::toResponseDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(response, "과목 목록을 조회했습니다."));

        } catch (Exception e) {
            log.error("과목 목록 조회 실패 - userUuid: {}", userUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 과목 제목 수정
     */
    @PutMapping("/{subjectUuid}")
    public ResponseEntity<ApiResponse<SubjectResponseDto>> updateSubject(
            @PathVariable UUID subjectUuid,
            @RequestBody Map<String, String> request) {

        try {
            String newTitle = request.get("title");
            Subject updatedSubject = subjectService.updateSubjectTitle(subjectUuid, newTitle);
            SubjectResponseDto response = subjectService.toResponseDto(updatedSubject);
            return ResponseEntity.ok(ApiResponse.success(response, "과목 제목이 수정되었습니다."));

        } catch (Exception e) {
            log.error("과목 수정 실패 - subjectUuid: {}", subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 과목 삭제
     */
    @DeleteMapping("/{subjectUuid}")
    public ResponseEntity<ApiResponse<String>> deleteSubject(@PathVariable UUID subjectUuid) {
        try {
            subjectService.deleteSubject(subjectUuid);
            return ResponseEntity.ok(ApiResponse.success("삭제 완료", "과목이 성공적으로 삭제되었습니다."));

        } catch (Exception e) {
            log.error("과목 삭제 실패 - subjectUuid: {}", subjectUuid, e);
            return ResponseEntity.ok(ApiResponse.error("과목 삭제에 실패했습니다: " + e.getMessage()));
        }
    }
}