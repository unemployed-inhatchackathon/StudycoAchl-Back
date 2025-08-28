package com.studycoAchl.hackaton.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    @Value("${app.file.upload-dir:uploads/audio}")
    private String uploadDir;

    /**
     * 오디오 파일 저장
     */
    public String saveAudioFile(MultipartFile file) throws IOException {
        try {
            // 업로드 디렉토리 생성
            createUploadDirectoryIfNotExists();

            // 고유한 파일명 생성
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());

            // 파일 경로 생성
            Path filePath = Paths.get(uploadDir, uniqueFileName);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath);

            log.info("파일 저장 완료 - 경로: {}", filePath.toString());
            return filePath.toString();

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new IOException("파일 저장에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 파일 삭제
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                log.info("파일 삭제 완료 - 경로: {}", filePath);
            } else {
                log.warn("파일을 찾을 수 없음 - 경로: {}", filePath);
            }

            return deleted;
        } catch (IOException e) {
            log.error("파일 삭제 실패 - 경로: {}", filePath, e);
            return false;
        }
    }

    /**
     * 업로드 디렉토리 생성
     */
    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
        }
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // 파일 확장자 추출
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        return String.format("audio_%s_%s%s", timestamp, uuid, extension);
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 파일 크기 조회
     */
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("파일 크기 조회 실패 - 경로: {}", filePath, e);
            return 0;
        }
    }
}
