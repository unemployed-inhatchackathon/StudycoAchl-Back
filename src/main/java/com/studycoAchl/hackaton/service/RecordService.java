package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.Record;
import com.studycoAchl.hackaton.entity.AppUsers;
import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.repository.RecordRepository;
import com.studycoAchl.hackaton.repository.UserRepository;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final STTService whisperSTTService;
    private final KeywordExtractionService keywordExtractionService;
    private final FileStorageService fileStorageService;

    /**
     * 음성 파일 업로드 및 Record 생성
     */
    public Record uploadAudioFile(UUID userUuid, String title, MultipartFile audioFile) {
        try {
            // 1. 파일 유효성 검증
            validateAudioFile(audioFile);

            // 2. 사용자 조회
            AppUsers user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userUuid));

            // 3. 파일 저장
            String filePath = fileStorageService.saveAudioFile(audioFile);

            // 4. Record 엔티티 생성
            Record record = Record.builder()
                    .title(title)
                    .filePath(filePath)
                    .fileSize(BigInteger.valueOf(audioFile.getSize()))
                    .duration(0) // 추후 계산
                    .appUsers(user)
                    .isFavorite(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return recordRepository.save(record);

        } catch (Exception e) {
            log.error("음성 파일 업로드 실패", e);
            throw new RuntimeException("음성 파일 업로드 실패: " + e.getMessage());
        }
    }

    /**
     * STT 변환 실행
     */
    public Map<String, Object> transcribeRecord(UUID recordId) {
        try {
            Record record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("녹음을 찾을 수 없습니다: " + recordId));

            // STT 변환
            String transcribedText = whisperSTTService.transcribe(record.getFilePath());

            // Record 업데이트
            record.setContentText(transcribedText);
            recordRepository.save(record);

            return Map.of(
                    "success", true,
                    "recordId", recordId,
                    "transcribedText", transcribedText,
                    "textLength", transcribedText.length()
            );

        } catch (Exception e) {
            log.error("STT 변환 실패 - recordId: {}", recordId, e);
            throw new RuntimeException("STT 변환 실패: " + e.getMessage());
        }
    }

    /**
     * 키워드 추출 및 문제 생성 준비
     */
    public Map<String, Object> extractKeywordsFromRecord(UUID recordId, UUID subjectId) {
        try {
            Record record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("녹음을 찾을 수 없습니다: " + recordId));

            if (record.getContentText() == null || record.getContentText().isEmpty()) {
                throw new RuntimeException("STT 변환이 완료되지 않았습니다.");
            }

            // 과목 조회 (선택사항)
            String subjectTitle = "일반학습";
            if (subjectId != null) {
                Subject subject = subjectRepository.findById(subjectId).orElse(null);
                if (subject != null) {
                    subjectTitle = subject.getTitle();
                }
            }

            // 키워드 추출
            List<String> keywords = keywordExtractionService.extractKeywordsFromMessage(
                    record.getContentText(),
                    subjectTitle
            );

            return Map.of(
                    "success", true,
                    "recordId", recordId,
                    "keywords", keywords,
                    "keywordCount", keywords.size(),
                    "canGenerateProblems", keywords.size() >= 3
            );

        } catch (Exception e) {
            log.error("키워드 추출 실패 - recordId: {}", recordId, e);
            throw new RuntimeException("키워드 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("음성 파일은 필수입니다.");
        }

        if (!whisperSTTService.isValidAudioFormat(file.getOriginalFilename())) {
            throw new IllegalArgumentException("지원하지 않는 오디오 형식입니다. (mp3, m4a, wav, flac 지원)");
        }

        // 파일 크기 제한 (25MB - Whisper 제한)
        if (file.getSize() > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 25MB를 초과할 수 없습니다.");
        }
    }

    public void deleteRecord(UUID recordId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("녹음을 찾을 수 없습니다: " + recordId));

        // 파일 삭제
        if (record.getFilePath() != null) {
            fileStorageService.deleteFile(record.getFilePath());
        }

        // DB에서 삭제
        recordRepository.delete(record);
    }

    public List<Record> getUserRecords(UUID userId) {
        try {
            AppUsers user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

            return recordRepository.findByAppUsersUuidOrderByCreatedAtDesc(user.getUuid());

        } catch (Exception e) {
            log.error("사용자 녹음 목록 조회 실패 - userId: {}", userId, e);
            throw new RuntimeException("녹음 목록 조회 실패: " + e.getMessage());
        }
    }
}
