package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecordRepository extends JpaRepository<Record, UUID> {

    // ========== 기본 조회 메소드들 - appUsers로 수정 ==========

    // 사용자별 기록 조회
    List<Record> findByAppUsers_Uuid(UUID userUuid);

    // 즐겨찾기 기록 조회
    List<Record> findByAppUsers_UuidAndIsFavoriteTrue(UUID userUuid);

    // 만료되지 않은 기록 조회
    List<Record> findByAppUsers_UuidAndIsExpiredIsNull(UUID userUuid);

    // 제목으로 기록 검색
    List<Record> findByAppUsers_UuidAndTitleContaining(UUID userUuid, String title);

    // 최신 기록 순으로 조회
    List<Record> findByAppUsersUuidOrderByCreatedAtDesc(UUID userUuid);
}