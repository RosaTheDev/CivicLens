package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.ZipRepresentativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepresentativeService {

    private final ZipRepresentativeRepository zipRepresentativeRepository;
    private final RepresentativeRepository representativeRepository;
    private final RepresentativeEnrichmentService enrichmentService;

    public List<Representative> getRepresentativesByZip(String zip) {
        String normalized = normalizeZip(zip);
        if (normalized == null) {
            return List.of();
        }
        enrichmentService.enrichFromApiIfNeeded(normalized);
        return zipRepresentativeRepository.findRepresentativesByZipCode(normalized);
    }

    public Representative getById(Long id) {
        return representativeRepository.findById(id).orElse(null);
    }

    /**
     * Normalize a ZIP code input to a 5-digit string (strip non-digits, take first 5).
     */
    private String normalizeZip(String zip) {
        if (zip == null) return null;
        String digitsOnly = zip.replaceAll("\\D", "");
        if (digitsOnly.length() < 5) {
            return null;
        }
        return digitsOnly.substring(0, 5);
    }
}
