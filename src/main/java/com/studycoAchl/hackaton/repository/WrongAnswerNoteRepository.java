package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.WrongAnswerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WrongAnswerNoteRepository extends JpaRepository<WrongAnswerNote, UUID> {

    // 기본 조회 - appUsers로 수정
    List<WrongAnswerNote> findByAppUsers_Uuid(UUID userUuid);
    List<WrongAnswerNote> findByAppUsers_UuidAndSubject_Uuid(UUID userUuid, UUID subjectUuid);

    // 복습 상태별 조회 - appUsers로 수정
    List<WrongAnswerNote> findByAppUsers_UuidAndIsMastered(UUID userUuid, Boolean isMastered);

    // 복습이 필요한 노트들 (@Query 방식은 이미 올바름)
    @Query("SELECT wan FROM WrongAnswerNote wan WHERE wan.appUsers.uuid = :userUuid AND wan.isMastered = false")
    List<WrongAnswerNote> findNotMasteredByUser(@Param("userUuid") UUID userUuid);

    @Query("SELECT wan FROM WrongAnswerNote wan WHERE wan.appUsers.uuid = :userUuid AND wan.subject.uuid = :subjectUuid AND wan.isMastered = false")
    List<WrongAnswerNote> findNotMasteredByUserAndSubject(@Param("userUuid") UUID userUuid, @Param("subjectUuid") UUID subjectUuid);

    // 키워드별 조회 - appUsers로 수정
    List<WrongAnswerNote> findByAppUsers_UuidAndKeyword(UUID userUuid, String keyword);
}