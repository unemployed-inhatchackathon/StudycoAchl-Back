package com.studycoAchl.hackaton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exams {

    @Id
    @Column(name = "UUID")
    private String uuid;

    @Column(name = "title")
    private String title;

    @Column(name = "pro_su")
    private Integer proSu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", referencedColumnName = "UUID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_uuid", referencedColumnName = "UUID")
    private Subject subject;
}