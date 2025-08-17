package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.service.QuestionGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired(required = false) // OpenAI 서비스가 없어도 오류 안나게 하기
    private QuestionGenerationService questionService;

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("서버 정상 작동!");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("테스트 성공!");
    }

    @GetMapping("/openai-test")
    public ResponseEntity<String> testOpenAI() {
        if (questionService == null) {
            return ResponseEntity.ok("QuestionGenerationService를 찾을 수 없습니다. OpenAI 설정을 확인해주세요.");
        }

        try {
            String result = questionService.generateQuestions(
                    "수학",
                    "이차함수 y = ax² + bx + c에서 a는 이차항의 계수이다.",
                    1
            );
            return ResponseEntity.ok("OpenAI 응답: " + result);
        } catch (Exception e) {
            return ResponseEntity.ok("OpenAI API 오류: " + e.getMessage());
        }
    }
}