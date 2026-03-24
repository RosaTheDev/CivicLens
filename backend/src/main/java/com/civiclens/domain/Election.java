package com.civiclens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "elections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "office_level", nullable = false, length = 16)
    private String officeLevel;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "election_type", nullable = false)
    private ElectionType electionType;

    @Column(name = "election_date", nullable = false)
    private LocalDate electionDate;

    @Column(nullable = false, length = 1000)
    private String description;
}
