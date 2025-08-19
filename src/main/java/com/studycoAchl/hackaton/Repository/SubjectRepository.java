package com.studycoAchl.hackaton.Repository;

import com.studycoAchl.hackaton.Entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    // 사용자별 과목 조회
    List<Subject> findByUser_Uuid(UUID userUuid);
    boolean existsByUserUuidAndName(UUID userUuid, String name);
}