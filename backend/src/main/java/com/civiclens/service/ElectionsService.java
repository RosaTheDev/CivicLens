package com.civiclens.service;

import com.civiclens.domain.Election;
import com.civiclens.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ElectionsService {

    private static final String FEDERAL_CODE = "US";
    private final ElectionRepository electionRepository;

    public List<ElectionDto> getUpcomingElections(String stateCode) {
        String normalizedState = normalizeStateCode(stateCode);
        List<String> filter = new ArrayList<>();
        filter.add(FEDERAL_CODE);
        if (normalizedState != null) {
            filter.add(normalizedState);
        }

        LocalDate today = LocalDate.now();
        List<Election> elections = electionRepository
                .findByStateCodeInAndElectionDateGreaterThanEqualOrderByElectionDateAsc(filter, today);

        return elections.stream()
                .map(e -> new ElectionDto(
                        e.getId(),
                        e.getStateCode(),
                        e.getOfficeLevel(),
                        e.getTitle(),
                        e.getElectionType().name(),
                        e.getElectionDate(),
                        ChronoUnit.DAYS.between(today, e.getElectionDate()),
                        e.getDescription()
                ))
                .toList();
    }

    private String normalizeStateCode(String stateCode) {
        if (stateCode == null || stateCode.isBlank()) {
            return null;
        }
        String normalized = stateCode.trim().toUpperCase(Locale.US);
        if (normalized.length() != 2) {
            return null;
        }
        return normalized;
    }

    public record ElectionDto(
            Long id,
            String stateCode,
            String officeLevel,
            String title,
            String electionType,
            LocalDate electionDate,
            long daysUntil,
            String description
    ) {}
}
