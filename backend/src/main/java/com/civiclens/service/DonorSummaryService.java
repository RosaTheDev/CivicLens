package com.civiclens.service;

import com.civiclens.domain.DonorSummary;
import com.civiclens.repository.DonorSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonorSummaryService {

    private final DonorSummaryRepository donorSummaryRepository;

    public DonorSummary getForRepresentative(Long representativeId, Integer cycleYear) {
        if (cycleYear != null) {
            return donorSummaryRepository.findByRepresentativeIdAndCycleYear(representativeId, cycleYear).orElse(null);
        }
        return donorSummaryRepository.findByRepresentativeId(representativeId).stream().findFirst().orElse(null);
    }
}
