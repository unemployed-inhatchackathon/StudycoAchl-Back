package com.studycoAchl.hackaton.Controller;


import com.studycoAchl.hackaton.DTO.CreateSubject;
import com.studycoAchl.hackaton.Entity.Subject;
import com.studycoAchl.hackaton.Service.SubjectService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SubjectController {
    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // 과목 생성
    @PostMapping("/users/{userUuid}/subjects")
    public ResponseEntity<Subject> createSubject(
            @PathVariable
            @Parameter
            UUID userUuid,
            @RequestBody String name) {  // JSON 대신 String

        Subject createdSubject = subjectService.createSubject(userUuid, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubject);
    }

    // 사용자의 과목 목록 조회
    @GetMapping("/users/{userUuid}/subjects")
    public ResponseEntity<List<Subject>> getUserSubjects(@PathVariable @Parameter UUID userUuid) {
        List<Subject> subjects = subjectService.getUserSubjects(userUuid);
        return ResponseEntity.ok(subjects);
    }
}
