package com.civiclens.repository;

import com.civiclens.domain.Election;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findByStateCodeInAndElectionDateGreaterThanEqualOrderByElectionDateAsc(
            List<String> stateCodes,
            LocalDate date
    );
}
