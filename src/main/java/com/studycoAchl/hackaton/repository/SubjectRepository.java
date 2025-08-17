package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {

    List<Subject> findByUserUuidOrderByCreatedAtDesc(String userUuid);

    List<Subject> findByTitleContainingAndUserUuid(String title, String userUuid);

    boolean existsByTitleAndUserUuid(String title, String userUuid);
}