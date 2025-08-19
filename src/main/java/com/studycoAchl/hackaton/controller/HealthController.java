package com.studycoAchl.hackaton.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Server is running!");
    }
}