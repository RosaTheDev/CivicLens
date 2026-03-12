package com.civiclens.repository;

import com.civiclens.domain.DonorSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonorSummaryRepository extends JpaRepository<DonorSummary, Long> {

    List<DonorSummary> findByRepresentativeId(Long representativeId);

    Optional<DonorSummary> findByRepresentativeIdAndCycleYear(Long representativeId, Integer cycleYear);
}
