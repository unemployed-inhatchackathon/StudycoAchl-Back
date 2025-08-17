package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.domain.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

    List<Record> findByUserUuidOrderByCreatedAtDesc(String userUuid);

    List<Record> findBySubjectUuidOrderByCreatedAtDesc(String subjectUuid);

    List<Record> findByUserUuidAndSubjectUuidOrderByCreatedAtDesc(String userUuid, String subjectUuid);

    // 즐겨찾기 기록들
    List<Record> findByUserUuidAndIsFavoriteTrueOrderByCreatedAtDesc(String userUuid);

    // 만료되지 않은 기록들
    @Query("SELECT r FROM Record r WHERE r.userUuid = :userUuid AND (r.expAt IS NULL OR r.expAt > :now) ORDER BY r.createdAt DESC")
    List<Record> findActiveRecordsByUserUuid(@Param("userUuid") String userUuid,
                                             @Param("now") LocalDateTime now);

    // 제목으로 검색
    List<Record> findByTitleContainingAndUserUuid(String title, String userUuid);

    // 내용으로 검색
    @Query("SELECT r FROM Record r WHERE r.userUuid = :userUuid AND (r.contentText LIKE %:keyword% OR r.aiText LIKE %:keyword%) ORDER BY r.createdAt DESC")
    List<Record> findByContentContaining(@Param("userUuid") String userUuid,
                                         @Param("keyword") String keyword);

    // 기간별 필터링
    List<Record> findByUserUuidAndCreatedAtBetweenOrderByCreatedAtDesc(String userUuid,
                                                                       LocalDateTime startDate,
                                                                       LocalDateTime endDate);
}