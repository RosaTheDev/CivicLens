package com.civiclens.client;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegislatorPhotoClient {

    private static final String DEFAULT_LEGISLATORS_CURRENT_URL =
            "https://unitedstates.github.io/congress-legislators/legislators-current.json";
    private static final String DEFAULT_LEGISLATORS_HISTORICAL_URL =
            "https://unitedstates.github.io/congress-legislators/legislators-historical.json";
    private static final String PHOTO_URL_TEMPLATE =
            "https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/%s.jpg";
    private static final Set<String> SUFFIXES = Set.of("jr", "sr", "ii", "iii", "iv", "v");
    private static final long CACHE_TTL_SECONDS = 6 * 60 * 60;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${civiclens.legislators.current-url:" + DEFAULT_LEGISLATORS_CURRENT_URL + "}")
    private String legislatorsCurrentUrl;
    @Value("${civiclens.legislators.historical-url:" + DEFAULT_LEGISLATORS_HISTORICAL_URL + "}")
    private String legislatorsHistoricalUrl;

    private volatile JsonNode cachedCurrentLegislators;
    private volatile Instant currentCacheLoadedAt;
    private volatile JsonNode cachedHistoricalLegislators;
    private volatile Instant historicalCacheLoadedAt;

    public String fetchMemberPhotoUrl(Representative representative) {
        if (representative == null || representative.getName() == null || representative.getState() == null) {
            return null;
        }
        String targetState = representative.getState().trim().toUpperCase(Locale.ROOT);
        String targetDistrict = representative.getDistrict() != null ? representative.getDistrict().trim() : null;
        String targetLastName = extractLastName(representative.getName());
        String targetFirstName = extractFirstName(representative.getName());
        if (targetLastName.isBlank()) {
            return null;
        }

        String fromCurrent = findPhotoUrlInDataset(
                loadLegislators(legislatorsCurrentUrl, true),
                representative,
                targetState,
                targetDistrict,
                targetLastName,
                targetFirstName,
                true
        );
        if (fromCurrent != null) {
            return fromCurrent;
        }

        return findPhotoUrlInDataset(
                loadLegislators(legislatorsHistoricalUrl, false),
                representative,
                targetState,
                targetDistrict,
                targetLastName,
                targetFirstName,
                false
        );
    }

    private JsonNode loadLegislators(String url, boolean currentDataset) {
        Instant loadedAt = currentDataset ? currentCacheLoadedAt : historicalCacheLoadedAt;
        JsonNode cached = currentDataset ? cachedCurrentLegislators : cachedHistoricalLegislators;
        if (cached != null && loadedAt != null && Instant.now().isBefore(loadedAt.plusSeconds(CACHE_TTL_SECONDS))) {
            return cached;
        }
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return cached;
            }
            JsonNode parsed = objectMapper.readTree(body);
            if (currentDataset) {
                cachedCurrentLegislators = parsed;
                currentCacheLoadedAt = Instant.now();
            } else {
                cachedHistoricalLegislators = parsed;
                historicalCacheLoadedAt = Instant.now();
            }
            return parsed;
        } catch (Exception e) {
            log.debug("Failed to load legislators dataset for photo lookup from {}: {}", url, e.getMessage());
            return cached;
        }
    }

    private String findPhotoUrlInDataset(
            JsonNode legislators,
            Representative representative,
            String targetState,
            String targetDistrict,
            String targetLastName,
            String targetFirstName,
            boolean requireCurrentTerm
    ) {
        if (legislators == null || !legislators.isArray()) {
            return null;
        }
        String targetFullName = normalizeFullName(representative.getName());
        for (JsonNode legislator : legislators) {
            if (!matchesName(legislator, targetLastName)) {
                continue;
            }
            boolean matches = requireCurrentTerm
                    ? hasMatchingCurrentTerm(legislator.path("terms"), representative.getChamber(), targetState, targetDistrict)
                    : hasAnyMatchingTerm(legislator.path("terms"), representative.getChamber(), targetState, targetDistrict);
            if (!matches && representative.getChamber() == Chamber.HOUSE && matchesFullName(legislator, targetFullName)) {
                matches = requireCurrentTerm
                        ? hasMatchingCurrentTermByState(legislator.path("terms"), representative.getChamber(), targetState)
                        : hasAnyMatchingTermByState(legislator.path("terms"), representative.getChamber(), targetState);
            }
            if (!matches && matchesStrongName(legislator, targetFirstName, targetLastName)) {
                matches = requireCurrentTerm
                        ? hasMatchingCurrentTermAnyChamberByState(legislator.path("terms"), targetState)
                        : hasAnyMatchingTermAnyChamberByState(legislator.path("terms"), targetState);
            }
            if (!matches) {
                continue;
            }
            String bioguideId = legislator.path("id").path("bioguide").asText("");
            if (bioguideId.isBlank()) {
                continue;
            }
            return PHOTO_URL_TEMPLATE.formatted(bioguideId.trim().toUpperCase(Locale.ROOT));
        }
        return null;
    }

    private boolean matchesName(JsonNode legislator, String targetLastName) {
        String last = cleanNameToken(legislator.path("name").path("last").asText(""));
        if (!last.isBlank() && last.equals(targetLastName)) {
            return true;
        }
        String official = legislator.path("name").path("official_full").asText("").toLowerCase(Locale.ROOT);
        return official.contains(targetLastName);
    }

    private boolean hasMatchingCurrentTerm(JsonNode terms, Chamber chamber, String state, String district) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        String chamberType = chamber == Chamber.SENATE ? "sen" : "rep";

        for (JsonNode term : terms) {
            if (!chamberType.equals(term.path("type").asText(""))) {
                continue;
            }
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (!state.equals(termState)) {
                continue;
            }
            if (!isCurrentTerm(term, today)) {
                continue;
            }

            if (chamber == Chamber.HOUSE) {
                String termDistrict = term.path("district").asText("");
                String normalizedTermDistrict = normalizeDistrict(termDistrict);
                String normalizedTargetDistrict = normalizeDistrict(district);
                if (normalizedTermDistrict == null || normalizedTargetDistrict == null) {
                    continue;
                }
                if (!normalizedTermDistrict.equals(normalizedTargetDistrict)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    private boolean hasAnyMatchingTerm(JsonNode terms, Chamber chamber, String state, String district) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        String chamberType = chamber == Chamber.SENATE ? "sen" : "rep";
        for (JsonNode term : terms) {
            if (!chamberType.equals(term.path("type").asText(""))) {
                continue;
            }
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (!state.equals(termState)) {
                continue;
            }
            if (chamber == Chamber.HOUSE) {
                String termDistrict = term.path("district").asText("");
                String normalizedTermDistrict = normalizeDistrict(termDistrict);
                String normalizedTargetDistrict = normalizeDistrict(district);
                if (normalizedTermDistrict == null || normalizedTargetDistrict == null) {
                    continue;
                }
                if (!normalizedTermDistrict.equals(normalizedTargetDistrict)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    private boolean hasMatchingCurrentTermByState(JsonNode terms, Chamber chamber, String state) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        String chamberType = chamber == Chamber.SENATE ? "sen" : "rep";
        for (JsonNode term : terms) {
            if (!chamberType.equals(term.path("type").asText(""))) {
                continue;
            }
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (!state.equals(termState)) {
                continue;
            }
            if (isCurrentTerm(term, today)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyMatchingTermByState(JsonNode terms, Chamber chamber, String state) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        String chamberType = chamber == Chamber.SENATE ? "sen" : "rep";
        for (JsonNode term : terms) {
            if (!chamberType.equals(term.path("type").asText(""))) {
                continue;
            }
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (state.equals(termState)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingCurrentTermAnyChamberByState(JsonNode terms, String state) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        for (JsonNode term : terms) {
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (!state.equals(termState)) {
                continue;
            }
            if (isCurrentTerm(term, today)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyMatchingTermAnyChamberByState(JsonNode terms, String state) {
        if (terms == null || !terms.isArray()) {
            return false;
        }
        for (JsonNode term : terms) {
            String termState = term.path("state").asText("").toUpperCase(Locale.ROOT);
            if (state.equals(termState)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentTerm(JsonNode term, LocalDate today) {
        String start = term.path("start").asText("");
        String end = term.path("end").asText("");
        if (start.isBlank() || end.isBlank()) {
            return false;
        }
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            return (today.isEqual(startDate) || today.isAfter(startDate))
                    && (today.isEqual(endDate) || today.isBefore(endDate));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String token = cleanNameToken(parts[i]);
            if (token.isBlank()) {
                continue;
            }
            if (SUFFIXES.contains(token)) {
                continue;
            }
            return token;
        }
        return "";
    }

    private String cleanNameToken(String token) {
        return token == null ? "" : token.replaceAll("[^A-Za-z\\-']", "").toLowerCase(Locale.ROOT);
    }

    private boolean matchesFullName(JsonNode legislator, String normalizedTargetFullName) {
        if (normalizedTargetFullName == null || normalizedTargetFullName.isBlank()) {
            return false;
        }
        String officialFull = normalizeFullName(legislator.path("name").path("official_full").asText(""));
        if (!officialFull.isBlank() && officialFull.equals(normalizedTargetFullName)) {
            return true;
        }
        String first = cleanNameToken(legislator.path("name").path("first").asText(""));
        String last = cleanNameToken(legislator.path("name").path("last").asText(""));
        String combined = (first + " " + last).trim();
        return !combined.isBlank() && combined.equals(normalizedTargetFullName);
    }

    private String normalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z\\s\\-']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean matchesStrongName(JsonNode legislator, String targetFirstName, String targetLastName) {
        if (targetFirstName == null || targetFirstName.isBlank() || targetLastName == null || targetLastName.isBlank()) {
            return false;
        }
        String legislatorFirst = cleanNameToken(legislator.path("name").path("first").asText(""));
        String legislatorLast = cleanNameToken(legislator.path("name").path("last").asText(""));
        if (legislatorFirst.isBlank() || legislatorLast.isBlank()) {
            return false;
        }
        return legislatorLast.equals(targetLastName)
                && (legislatorFirst.equals(targetFirstName)
                || legislatorFirst.startsWith(targetFirstName)
                || targetFirstName.startsWith(legislatorFirst));
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 0) {
            return "";
        }
        return cleanNameToken(parts[0]);
    }

    private String normalizeDistrict(String district) {
        if (district == null || district.isBlank()) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(district.replaceAll("[^0-9]", "")));
        } catch (Exception ignored) {
            return null;
        }
    }
}
