package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, String> {

    @Query("SELECT r FROM Record r WHERE r.user.uuid = :userUuid")
    List<Record> findByUserUuid(@Param("userUuid") String userUuid);

    @Query("SELECT r FROM Record r WHERE r.user.uuid = :userUuid AND r.isFavorite = true")
    List<Record> findByUserUuidAndIsFavoriteTrue(@Param("userUuid") String userUuid);

    @Query("SELECT r FROM Record r WHERE r.user.uuid = :userUuid AND r.isExpired IS NULL")
    List<Record> findByUserUuidAndNotExpired(@Param("userUuid") String userUuid);

    @Query("SELECT r FROM Record r WHERE r.user.uuid = :userUuid AND r.title LIKE %:title%")
    List<Record> findByUserUuidAndTitleContaining(@Param("userUuid") String userUuid, @Param("title") String title);

    @Query("SELECT r FROM Record r WHERE r.user.uuid = :userUuid ORDER BY r.createdAt DESC")
    List<Record> findByUserUuidOrderByCreatedAtDesc(@Param("userUuid") String userUuid);
}