package com.studycoAchl.hackaton.controller;

import com.studycoAchl.hackaton.dto.ApiResponse;
import com.studycoAchl.hackaton.entity.Problem;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    @PostMapping("/upload")
    public ApiResponse<Record> uploadRecord(@RequestParam MultipartFile file){
        return null;
    }

    @PostMapping("/{recordId}/transcribe")
    public ApiResponse<String> transcribeRecord(@PathVariable UUID recordId){
        return null;
    }

    @PostMapping("/{recordId}/generate-problems")
    public ApiResponse<Problem> generateProblems(@PathVariable UUID recordId){
        return null;
    }
}
