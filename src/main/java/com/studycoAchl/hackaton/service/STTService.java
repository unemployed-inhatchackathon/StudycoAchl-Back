package com.studycoAchl.hackaton.service;

import org.springframework.web.multipart.MultipartFile;

public interface STTService{
    String transcribe(String filePath);
    String transcirbe(MultipartFile file);
}
