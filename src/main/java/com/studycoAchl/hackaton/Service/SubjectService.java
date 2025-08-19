package com.studycoAchl.hackaton.Service;

import com.studycoAchl.hackaton.Entity.Subject;
import com.studycoAchl.hackaton.Entity.User;
import com.studycoAchl.hackaton.Repository.SubjectRepository;
import com.studycoAchl.hackaton.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public SubjectService(SubjectRepository subjectRepository, UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    // 과목 생성
    public Subject createSubject(UUID userUuid, String name) {
        validateSubjectName(name);

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 중복 체크
        if (subjectRepository.existsByUserUuidAndName(userUuid, name)) {
            throw new IllegalArgumentException("이미 존재하는 과목명입니다.");
        }

        Subject subject = new Subject(user, name.trim());

        return subjectRepository.save(subject);
    }

    // 사용자의 모든 과목 조회
    public List<Subject> getUserSubjects(UUID userUuid) {
        return subjectRepository.findByUser_Uuid(userUuid);
    }

    // 과목 존재 여부 확인
    public boolean subjectExists(UUID subjectUuid) {
        return subjectRepository.existsById(subjectUuid);
    }

    // 과목명 검증
    private void validateSubjectName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("과목명은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("과목명은 50자를 초과할 수 없습니다.");
        }
    }
}
