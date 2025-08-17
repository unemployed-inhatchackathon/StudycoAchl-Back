package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.service.ProblemSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/problem-session")
public class ProblemSessionController {

    @Autowired
    private ProblemSessionService sessionService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("문제풀이 세션 API 정상 작동!");
    }

    @GetMapping("/test-start")
    public ResponseEntity<Map<String, Object>> testStartSession() {

        Map<String, Object> result = sessionService.createProblemSession(
                "test-user-uuid",
                "test-subject-uuid",
                "테스트 수학 문제",
                3,
                "manual",
                "수학 사칙연산 기본 문제를 만들어주세요"
        );

        return ResponseEntity.ok(result);
    }
}