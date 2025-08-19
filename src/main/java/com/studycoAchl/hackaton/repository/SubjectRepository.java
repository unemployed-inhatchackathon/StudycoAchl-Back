package com.studycoAchl.hackaton.repository;

import com.studycoAchl.hackaton.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {

    @Query("SELECT s FROM Subject s WHERE s.user.uuid = :userUuid")
    List<Subject> findByUserUuid(@Param("userUuid") String userUuid);

    List<Subject> findByTitleContaining(String title);

    @Query("SELECT s FROM Subject s WHERE s.user.uuid = :userUuid AND s.title = :title")
    Optional<Subject> findByUserUuidAndTitle(@Param("userUuid") String userUuid, @Param("title") String title);

    @Query("SELECT s FROM Subject s ORDER BY s.createdAt DESC")
    List<Subject> findRecentSubjects();

    @Query("SELECT s FROM Subject s WHERE s.user.uuid = :userUuid ORDER BY s.createdAt DESC")
    List<Subject> findRecentSubjectsByUser(@Param("userUuid") String userUuid);
}