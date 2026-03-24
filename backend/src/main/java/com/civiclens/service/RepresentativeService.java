package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.civiclens.client.CongressGovClient;
import com.civiclens.client.LegislatorPhotoClient;
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
    private final CongressGovClient congressGovClient;
    private final LegislatorPhotoClient legislatorPhotoClient;

    public List<Representative> getRepresentativesByZip(String zip) {
        String normalized = normalizeZip(zip);
        if (normalized == null) {
            return List.of();
        }
        enrichmentService.enrichFromApiIfNeeded(normalized);
        List<Representative> reps = zipRepresentativeRepository.findRepresentativesByZipCode(normalized);
        maybeHydratePhotoUrls(reps);
        return reps;
    }

    public Representative getById(Long id) {
        Representative rep = representativeRepository.findById(id).orElse(null);
        if (rep != null) {
            maybeHydratePhotoUrls(List.of(rep));
        }
        return rep;
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

    private void maybeHydratePhotoUrls(List<Representative> reps) {
        for (Representative rep : reps) {
            if (rep == null) {
                continue;
            }
            String existing = rep.getPhotoUrl();
            if (existing != null && !existing.isBlank()) {
                String normalized = normalizeLegacyPhotoUrl(existing);
                if (!existing.equals(normalized)) {
                    rep.setPhotoUrl(normalized);
                    representativeRepository.save(rep);
                }
                continue;
            }
            String photoUrl = congressGovClient.fetchMemberPhotoUrl(rep);
            if (photoUrl == null || photoUrl.isBlank()) {
                photoUrl = legislatorPhotoClient.fetchMemberPhotoUrl(rep);
            }
            if (photoUrl == null || photoUrl.isBlank()) {
                continue;
            }
            rep.setPhotoUrl(photoUrl);
            representativeRepository.save(rep);
        }
    }

    private String normalizeLegacyPhotoUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return photoUrl;
        }
        String marker = "/congress/225x275/";
        int idx = photoUrl.indexOf(marker);
        if (idx < 0) {
            return photoUrl;
        }
        String fileName = photoUrl.substring(idx + marker.length());
        if (fileName.isBlank()) {
            return photoUrl;
        }
        return "https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/" + fileName;
    }
}
