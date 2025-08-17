package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.Exams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamsRepository extends JpaRepository<Exams, String> {

    List<Exams> findByUserUuidOrderByCreatedAtDesc(String userUuid);

    List<Exams> findBySubjectUuidOrderByCreatedAtDesc(String subjectUuid);

    List<Exams> findByUserUuidAndSubjectUuidOrderByCreatedAtDesc(String userUuid, String subjectUuid);

    List<Exams> findByTitleContainingAndUserUuid(String title, String userUuid);

    // 문제 수별 필터링
    List<Exams> findByProSuAndUserUuid(Integer proSu, String userUuid);

    // 특정 범위의 문제 수 필터링
    @Query("SELECT e FROM Exams e WHERE e.userUuid = :userUuid AND e.proSu BETWEEN :minProSu AND :maxProSu ORDER BY e.createdAt DESC")
    List<Exams> findByUserUuidAndProSuBetween(@Param("userUuid") String userUuid,
                                              @Param("minProSu") Integer minProSu,
                                              @Param("maxProSu") Integer maxProSu);
}