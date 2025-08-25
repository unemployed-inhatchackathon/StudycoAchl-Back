package com.studycoAchl.hackaton.service;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final OpenAiService openAiService;

    public String transcribe(MultipartFile audioFile){
        CreateTranscriptionRequest request = CreateTranscriptionRequest.builder()
                .model("whisper-1")
                .build();
        return null;//openAiService.createTranscription(request, audioFile).getText();
    }
}

