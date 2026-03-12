package com.civiclens.service;

import com.civiclens.client.WhoIsMyRepClient;
import com.civiclens.client.WhoIsMyRepResponse;
import com.civiclens.domain.Chamber;
import com.civiclens.domain.Party;
import com.civiclens.domain.Representative;
import com.civiclens.domain.ZipRepresentative;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.ZipRepresentativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Enriches representative data from the Who Is My Representative API:
 * when a zip has no reps in the DB, fetches from the API and persists them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepresentativeEnrichmentService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]");

    private final WhoIsMyRepClient whoIsMyRepClient;
    private final RepresentativeRepository representativeRepository;
    private final ZipRepresentativeRepository zipRepresentativeRepository;

    /**
     * If the database has no representatives for this zip, fetch from the external API,
     * map to our domain, persist, and return true. Otherwise return false.
     */
    @Transactional
    public boolean enrichFromApiIfNeeded(String zip) {
        if (zip == null || zip.isBlank()) {
            return false;
        }
        String trimmed = zip.trim();
        List<Representative> existing = zipRepresentativeRepository.findRepresentativesByZipCode(trimmed);
        if (!existing.isEmpty()) {
            return false;
        }
        List<WhoIsMyRepResponse.Result> results = whoIsMyRepClient.fetchByZip(trimmed);
        if (results.isEmpty()) {
            return false;
        }
        for (WhoIsMyRepResponse.Result r : results) {
            String externalId = toExternalId(r);
            Representative rep = representativeRepository.findByExternalId(externalId)
                    .orElseGet(() -> {
                        Representative newRep = toRepresentative(r, externalId);
                        return representativeRepository.save(newRep);
                    });
            if (!zipRepresentativeRepository.existsByZipCodeAndRepresentativeId(trimmed, rep.getId())) {
                zipRepresentativeRepository.save(ZipRepresentative.builder()
                        .zipCode(trimmed)
                        .representative(rep)
                        .build());
            }
        }
        log.debug("Enriched representatives for zip {} from Who Is My Representative API", trimmed);
        return true;
    }

    private static String toExternalId(WhoIsMyRepResponse.Result r) {
        String state = (r.getState() != null) ? r.getState().trim().toUpperCase() : "";
        boolean isSenate = r.getDistrict() == null || r.getDistrict().isBlank();
        if (isSenate) {
            String slug = (r.getName() != null)
                    ? NON_ALNUM.matcher(r.getName()).replaceAll("").toUpperCase()
                    : "";
            return "senate-" + state + "-" + slug;
        }
        String district = (r.getDistrict() != null) ? r.getDistrict().trim() : "";
        return "house-" + state + "-" + district;
    }

    private static Representative toRepresentative(WhoIsMyRepResponse.Result r, String externalId) {
        boolean isSenate = r.getDistrict() == null || r.getDistrict().isBlank();
        Chamber chamber = isSenate ? Chamber.SENATE : Chamber.HOUSE;
        String district = isSenate ? null : (r.getDistrict() != null ? r.getDistrict().trim() : null);
        Party party = mapParty(r.getParty());
        return Representative.builder()
                .externalId(externalId)
                .name(r.getName() != null ? r.getName().trim() : "Unknown")
                .chamber(chamber)
                .state(r.getState() != null && !r.getState().isBlank() ? r.getState().trim().toUpperCase() : "XX")
                .district(district)
                .party(party)
                .photoUrl(null)
                .officialUrl(r.getLink() != null ? r.getLink().trim() : null)
                .build();
    }

    private static Party mapParty(String apiParty) {
        if (apiParty == null || apiParty.isBlank()) {
            return Party.OTHER;
        }
        String p = apiParty.trim().toLowerCase();
        if (p.contains("democrat")) return Party.DEMOCRATIC;
        if (p.contains("republican")) return Party.REPUBLICAN;
        if (p.contains("independent")) return Party.INDEPENDENT;
        return Party.OTHER;
    }
}
