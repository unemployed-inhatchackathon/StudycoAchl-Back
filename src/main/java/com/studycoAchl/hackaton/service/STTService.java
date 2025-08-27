package com.studycoAchl.hackaton.service;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class STTService {
    private final OpenAiService openAiService;

    @Value("${app.use-real-ai:true}")
    private boolean useRealAi;

    /**
     * MultipartFile을 STT 변환
     */
    public String transcribe(MultipartFile audioFile) {
        if (!useRealAi) {
            return generateMockTranscription(audioFile.getOriginalFilename());
        }

        File tempFile = null;
        try {
            tempFile = convertMultipartFileToFile(audioFile);

            CreateTranscriptionRequest request = CreateTranscriptionRequest.builder()
                    .model("whisper-1")
                    .language("ko")
                    .responseFormat("text") // 이미 text로 설정됨
                    .build();

            // Whisper는 순수 텍스트를 반환함
            String transcription = openAiService.createTranscription(request, tempFile).getText();

            log.info("Whisper STT 변환 완료: {}", transcription);
            return transcription; // 추가 파싱 없이 그대로 반환

        } catch (Exception e) {
            log.error("STT 변환 실패", e);
            throw new RuntimeException("STT 변환 실패: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 파일 경로로 STT 변환
     */
    public String transcribe(String filePath) {
        if (!useRealAi) {
            return generateMockTranscription(filePath);
        }

        try {
            File audioFile = new File(filePath);

            CreateTranscriptionRequest request = CreateTranscriptionRequest.builder()
                    .model("whisper-1")
                    .language("ko")
                    .responseFormat("json")
                    .build();

            String transcription = openAiService.createTranscription(request, audioFile).getText();

            log.info("Whisper STT 변환 완료 - 파일: {}", filePath);
            return transcription;

        } catch (Exception e) {
            log.error("STT 변환 실패 - 파일: {}", filePath, e);
            throw new RuntimeException("STT 변환 실패: " + e.getMessage());
        }
    }

    /**
     * MultipartFile을 File로 변환
     */
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File tempFile = File.createTempFile("audio_", "_" + multipartFile.getOriginalFilename());

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        return tempFile;
    }

    /**
     * 모의 STT 응답 생성
     */
    private String generateMockTranscription(String fileName) {
        return String.format("모의 STT 변환 결과입니다. 파일명: %s. 실제 환경에서는 Whisper API를 통해 정확한 음성 인식 결과를 제공합니다.", fileName);
    }

    /**
     * 지원되는 오디오 형식 검증
     */
    public boolean isValidAudioFormat(String fileName) {
        if (fileName == null) return false;

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".mp3") ||
                lowerFileName.endsWith(".m4a") ||
                lowerFileName.endsWith(".wav") ||
                lowerFileName.endsWith(".flac") ||
                lowerFileName.endsWith(".mp4") ||
                lowerFileName.endsWith(".webm");
    }
}
