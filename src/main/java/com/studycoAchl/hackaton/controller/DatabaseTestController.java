package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.domain.*;
import com.studycoAchl.hackaton.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/database-test")
public class DatabaseTestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("데이터베이스 테스트 컨트롤러 정상 작동!");
    }

    @PostMapping("/create-test-user")
    public ResponseEntity<String> createTestUser() {
        try {
            String userUuid = UUID.randomUUID().toString();
            User testUser = new User(userUuid, "test@example.com", "테스트유저");
            User savedUser = userRepository.save(testUser);

            return ResponseEntity.ok("테스트 사용자 생성 완료: " + savedUser.getUuid());
        } catch (Exception e) {
            return ResponseEntity.ok("사용자 생성 실패: " + e.getMessage());
        }
    }

    @GetMapping("/count-users")
    public ResponseEntity<String> countUsers() {
        try {
            long userCount = userRepository.count();
            return ResponseEntity.ok("현재 사용자 수: " + userCount + "명");
        } catch (Exception e) {
            return ResponseEntity.ok("사용자 조회 실패: " + e.getMessage());
        }
    }
}