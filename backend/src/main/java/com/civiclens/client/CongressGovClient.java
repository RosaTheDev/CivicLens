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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class CongressGovClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${civiclens.congress.api-base-url:https://api.congress.gov/v3}")
    private String apiBaseUrl;

    @Value("${civiclens.congress.api-key:}")
    private String apiKey;

    public static class BillSummary {
        public final String title;
        public final String description;
        public final String congressGovUrl;

        public BillSummary(String title, String description, String congressGovUrl) {
            this.title = title;
            this.description = description;
            this.congressGovUrl = congressGovUrl;
        }
    }

    public List<BillSummary> fetchRecentSponsoredBills(Representative representative, int limit) {
        if (apiKey == null || apiKey.isBlank() || representative == null) {
            return Collections.emptyList();
        }
        try {
            String bioguideId = resolveBioguideId(representative);
            if (bioguideId == null) {
                return Collections.emptyList();
            }
            return fetchSponsoredLegislation(bioguideId, limit);
        } catch (Exception e) {
            log.warn("Congress.gov API failed for representative {}: {}", representative.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<BillSummary> fetchRecentCosponsoredBills(Representative representative, int limit) {
        if (apiKey == null || apiKey.isBlank() || representative == null) {
            return Collections.emptyList();
        }
        try {
            String bioguideId = resolveBioguideId(representative);
            if (bioguideId == null) {
                return Collections.emptyList();
            }
            return fetchCosponsoredLegislation(bioguideId, limit);
        } catch (Exception e) {
            log.warn("Congress.gov API (cosponsored) failed for representative {}: {}", representative.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    public String fetchMemberPhotoUrl(Representative representative) {
        if (apiKey == null || apiKey.isBlank() || representative == null) {
            return null;
        }
        try {
            JsonNode member = resolveMemberNode(representative);
            if (member == null) {
                return null;
            }
            String imageUrl = member.path("depiction").path("imageUrl").asText(null);
            if (imageUrl != null && !imageUrl.isBlank()) {
                return imageUrl.trim();
            }
            String bioguideId = member.path("bioguideId").asText(null);
            if (bioguideId == null || bioguideId.isBlank()) {
                return null;
            }
            return "https://theunitedstates.io/images/congress/225x275/" + bioguideId.trim().toUpperCase(Locale.ROOT) + ".jpg";
        } catch (Exception e) {
            log.warn("Congress.gov API photo lookup failed for representative {}: {}", representative.getName(), e.getMessage());
            return null;
        }
    }

    private String resolveBioguideId(Representative representative) throws URISyntaxException {
        JsonNode member = resolveMemberNode(representative);
        if (member == null) {
            return null;
        }
        String bioguideId = member.path("bioguideId").asText(null);
        return (bioguideId == null || bioguideId.isBlank()) ? null : bioguideId;
    }

    private JsonNode resolveMemberNode(Representative representative) throws URISyntaxException {
        String stateCode = representative.getState();
        if (stateCode == null || stateCode.isBlank()) {
            return null;
        }
        String chamberFilter = representative.getChamber() == Chamber.SENATE ? "Senate" : "House of Representatives";
        StringBuilder url = new StringBuilder();
        url.append(apiBaseUrl)
                .append("/member/")
                .append(stateCode.toUpperCase(Locale.ROOT))
                .append("?currentMember=true&limit=100&api_key=")
                .append(apiKey);

        String response = restTemplate.getForObject(new URI(url.toString()), String.class);
        if (response == null || response.isBlank()) {
            return null;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to parse Congress.gov member response: {}", e.getMessage());
            return null;
        }
        JsonNode members = root.path("members");
        if (!members.isArray()) {
            return null;
        }
        String repLastName = lastNameOf(representative.getName());
        for (JsonNode member : members) {
            String invertedName = member.path("invertedOrderName").asText(""); // "Last, First"
            String chamber = firstChamberFromTerms(member);
            if (!chamberFilter.equals(chamber)) {
                continue;
            }
            if (!repLastName.isEmpty() && !invertedName.toLowerCase(Locale.ROOT).startsWith(repLastName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            return member;
        }
        return null;
    }

    private String firstChamberFromTerms(JsonNode member) {
        JsonNode terms = member.path("terms").path("item");
        if (terms.isArray() && terms.size() > 0) {
            return terms.get(0).path("chamber").asText("");
        } else if (terms.isObject()) {
            return terms.path("chamber").asText("");
        }
        return "";
    }

    private String lastNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private List<BillSummary> fetchSponsoredLegislation(String bioguideId, int limit) throws URISyntaxException {
        String url = apiBaseUrl + "/member/" + bioguideId + "/sponsored-legislation?api_key=" + apiKey + "&limit=" + Math.max(limit, 3);
        String response = restTemplate.getForObject(new URI(url), String.class);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to parse Congress.gov sponsored-legislation response: {}", e.getMessage());
            return Collections.emptyList();
        }
        JsonNode sponsored = root.path("sponsoredLegislation").path("item");
        if (!sponsored.isArray()) {
            return Collections.emptyList();
        }
        return toBillSummaries(sponsored, limit);
    }

    private List<BillSummary> fetchCosponsoredLegislation(String bioguideId, int limit) throws URISyntaxException {
        String url = apiBaseUrl + "/member/" + bioguideId + "/cosponsored-legislation?api_key=" + apiKey + "&limit=" + Math.max(limit, 3);
        String response = restTemplate.getForObject(new URI(url), String.class);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to parse Congress.gov cosponsored-legislation response: {}", e.getMessage());
            return Collections.emptyList();
        }
        JsonNode cosponsored = root.path("cosponsoredLegislation").path("item");
        if (!cosponsored.isArray()) {
            return Collections.emptyList();
        }
        return toBillSummaries(cosponsored, limit);
    }

    private List<BillSummary> toBillSummaries(JsonNode items, int limit) {
        List<BillSummary> result = new ArrayList<>();
        for (JsonNode item : items) {
            String title = item.path("latestTitle").asText("Untitled bill");
            String number = item.path("number").asText("");
            String type = item.path("type").asText("");
            String congress = item.path("congress").asText("");
            String actionText = item.path("latestAction").path("text").asText("");
            String billUrl = item.path("url").asText("");

            String congressGovUrl = billUrl;
            if (!congress.isEmpty() && !type.isEmpty() && !number.isEmpty()) {
                congressGovUrl = String.format("https://www.congress.gov/bill/%sth-congress/%s/%s",
                        congress,
                        type.toLowerCase(Locale.ROOT),
                        number);
            }

            String description = actionText.isEmpty()
                    ? "See latest actions and details on Congress.gov."
                    : actionText;
            result.add(new BillSummary(title, description, congressGovUrl));
            if (result.size() >= limit) break;
        }
        return result;
    }
}

