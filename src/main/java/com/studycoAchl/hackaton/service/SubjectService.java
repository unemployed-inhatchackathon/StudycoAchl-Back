package com.studycoAchl.hackaton.service;

import com.studycoAchl.hackaton.entity.Subject;
import com.studycoAchl.hackaton.entity.AppUsers;
import com.studycoAchl.hackaton.repository.SubjectRepository;
import com.studycoAchl.hackaton.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    // ========== 과목 생성 및 관리 ==========
    public Subject createSubject(UUID userUuid, String title) {
        validateSubjectTitle(title);

        // 사용자 존재 여부 검증
        AppUsers user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 중복 체크
        if (subjectRepository.existsByUserUuidAndTitle(userUuid, title)) {
            throw new IllegalArgumentException("이미 존재하는 과목명입니다.");
        }

        Subject subject = Subject.builder()
                .userUuid(user.getUuid())
                .title(title.trim())
                .build();

        return subjectRepository.save(subject);
    }

    // ========== 조회 ==========
    public Subject findById(UUID subjectUuid) {
        return subjectRepository.findById(subjectUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다."));
    }

    public List<Subject> getUserSubjects(UUID userUuid) {
        return subjectRepository.findByUserUuid(userUuid);
    }

    public List<Subject> getUserSubjectsOrderByCreated(UUID userUuid) {
        return subjectRepository.findByUserUuidOrderByCreatedAtDesc(userUuid);
    }

    public List<Subject> searchSubjectsByTitle(String title) {
        return subjectRepository.findByTitleContaining(title);
    }

    // ========== 존재 여부 확인 ==========
    public boolean subjectExists(UUID subjectUuid) {
        return subjectRepository.existsById(subjectUuid);
    }

    public boolean subjectExistsByUserAndTitle(UUID userUuid, String title) {
        return subjectRepository.existsByUserUuidAndTitle(userUuid, title);
    }

    // ========== 수정 및 삭제 ==========
    public Subject updateSubjectTitle(UUID subjectUuid, String newTitle) {
        validateSubjectTitle(newTitle);

        Subject subject = findById(subjectUuid);

        // 같은 사용자의 다른 과목과 중복되는지 확인
        if (!subject.getTitle().equals(newTitle) &&
                subjectExistsByUserAndTitle(subject.getUserUuid(), newTitle)) {
            throw new IllegalArgumentException("이미 존재하는 과목명입니다.");
        }

        subject.setTitle(newTitle.trim());
        return subjectRepository.save(subject);
    }

    public void deleteSubject(UUID subjectUuid) {
        Subject subject = findById(subjectUuid);
        subjectRepository.delete(subject);
    }

    // ========== 검증 ==========
    private void validateSubjectTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("과목명은 필수입니다.");
        }
        if (title.length() > 50) {
            throw new IllegalArgumentException("과목명은 50자를 초과할 수 없습니다.");
        }
    }
}
