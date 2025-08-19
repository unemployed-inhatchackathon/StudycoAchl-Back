package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Exams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamsRepository extends JpaRepository<Exams, String> {

    @Query("SELECT e FROM Exams e WHERE e.user.uuid = :userUuid")
    List<Exams> findByUserUuid(@Param("userUuid") String userUuid);

    @Query("SELECT e FROM Exams e WHERE e.subject.uuid = :subjectUuid")
    List<Exams> findBySubjectUuid(@Param("subjectUuid") String subjectUuid);

    List<Exams> findByTitleContaining(String title);
}