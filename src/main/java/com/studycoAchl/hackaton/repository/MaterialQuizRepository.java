package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.MaterialQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialQuizRepository extends JpaRepository<MaterialQuiz, UUID> {

    // 학습자료별 퀴즈 조회
    List<MaterialQuiz> findByStudyMaterial_Uuid(UUID materialUuid);

    // 사용자별 퀴즈 조회
    List<MaterialQuiz> findByAppUsers_Uuid(UUID userUuid);

    // 과목별 퀴즈 조회
    List<MaterialQuiz> findBySubject_Uuid(UUID subjectUuid);

    // 사용자 + 과목별 퀴즈 조회
    List<MaterialQuiz> findByAppUsers_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);

    // 난이도별 조회
    List<MaterialQuiz> findByDifficulty(String difficulty);

    // 생성 방법별 조회
    List<MaterialQuiz> findByGenerationMethod(String generationMethod);

    // 최신순 정렬
    List<MaterialQuiz> findByAppUsers_UuidOrderByCreatedAtDesc(UUID userUuid);
}