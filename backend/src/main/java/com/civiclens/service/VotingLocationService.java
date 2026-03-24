package com.civiclens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VotingLocationService {

    private static final Pattern STATE_CODE_WITH_ZIP = Pattern.compile("\\b([A-Za-z]{2})\\s+\\d{5}(?:-\\d{4})?\\b");
    private static final Pattern ZIP_PATTERN = Pattern.compile("\\b(\\d{5})(?:-\\d{4})?\\b");
    private static final String ZIPPO_BASE = "https://api.zippopotam.us/us/";
    private static final String NOMINATIM_SEARCH = "https://nominatim.openstreetmap.org/search";
    private static final String NC_POLLING_LOOKUP = "https://vt.ncsbe.gov/PPLkup/PollingPlaceResult/";
    private static final String GOOGLE_CIVIC_BASE = "https://www.googleapis.com/civicinfo/v2/voterinfo";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${civiclens.voting.google-civic-api-key:}")
    private String googleCivicApiKey;

    private static final Map<String, String> STATE_ELECTION_OFFICE_SITES = Map.ofEntries(
            Map.entry("AL", "https://www.sos.alabama.gov/alabama-votes"),
            Map.entry("AK", "https://www.elections.alaska.gov"),
            Map.entry("AZ", "https://azsos.gov/elections"),
            Map.entry("AR", "https://www.sos.arkansas.gov/elections"),
            Map.entry("CA", "https://www.sos.ca.gov/elections"),
            Map.entry("CO", "https://www.sos.state.co.us/pubs/elections/main.html"),
            Map.entry("CT", "https://portal.ct.gov/sots/election-services"),
            Map.entry("DC", "https://dcboe.org"),
            Map.entry("DE", "https://elections.delaware.gov"),
            Map.entry("FL", "https://dos.myflorida.com/elections"),
            Map.entry("GA", "https://sos.ga.gov/page/elections-and-registration"),
            Map.entry("HI", "https://elections.hawaii.gov"),
            Map.entry("IA", "https://sos.iowa.gov/elections"),
            Map.entry("ID", "https://voteidaho.gov"),
            Map.entry("IL", "https://www.elections.il.gov"),
            Map.entry("IN", "https://www.in.gov/sos/elections"),
            Map.entry("KS", "https://sos.ks.gov/elections"),
            Map.entry("KY", "https://elect.ky.gov"),
            Map.entry("LA", "https://www.sos.la.gov/ElectionsAndVoting"),
            Map.entry("MA", "https://www.sec.state.ma.us/divisions/elections"),
            Map.entry("MD", "https://elections.maryland.gov"),
            Map.entry("ME", "https://www.maine.gov/sos/cec/elec"),
            Map.entry("MI", "https://www.michigan.gov/sos/elections"),
            Map.entry("MN", "https://www.sos.state.mn.us/elections-voting"),
            Map.entry("MO", "https://www.sos.mo.gov/elections"),
            Map.entry("MS", "https://www.sos.ms.gov/elections-voting"),
            Map.entry("MT", "https://sosmt.gov/elections"),
            Map.entry("NC", "https://www.ncsbe.gov"),
            Map.entry("ND", "https://vip.sos.nd.gov"),
            Map.entry("NE", "https://sos.nebraska.gov/elections"),
            Map.entry("NH", "https://www.sos.nh.gov/elections"),
            Map.entry("NJ", "https://www.nj.gov/state/elections"),
            Map.entry("NM", "https://www.sos.nm.gov/voting-and-elections"),
            Map.entry("NV", "https://www.nvsos.gov/sos/elections"),
            Map.entry("NY", "https://elections.ny.gov"),
            Map.entry("OH", "https://www.ohiosos.gov/elections"),
            Map.entry("OK", "https://oklahoma.gov/elections"),
            Map.entry("OR", "https://sos.oregon.gov/voting-elections"),
            Map.entry("PA", "https://www.pa.gov/en/services/vote.html"),
            Map.entry("RI", "https://vote.sos.ri.gov"),
            Map.entry("SC", "https://scvotes.gov"),
            Map.entry("SD", "https://sdsos.gov/elections-voting"),
            Map.entry("TN", "https://sos.tn.gov/elections"),
            Map.entry("TX", "https://www.sos.state.tx.us/elections"),
            Map.entry("UT", "https://vote.utah.gov"),
            Map.entry("VA", "https://www.elections.virginia.gov"),
            Map.entry("VT", "https://sos.vermont.gov/elections"),
            Map.entry("WA", "https://www.sos.wa.gov/elections"),
            Map.entry("WI", "https://elections.wi.gov"),
            Map.entry("WV", "https://sos.wv.gov/elections"),
            Map.entry("WY", "https://sos.wyo.gov/elections"),
            Map.entry("AS", "https://www.vote.gov/american-samoa/"),
            Map.entry("GU", "https://www.vote.gov/guam/"),
            Map.entry("MP", "https://www.vote.gov/northern-mariana-islands/"),
            Map.entry("PR", "https://www.vote.gov/puerto-rico/"),
            Map.entry("VI", "https://www.vote.gov/virgin-islands/")
    );

    private static final Map<String, String> JURISDICTION_NAME_TO_CODE = Map.ofEntries(
            Map.entry("alabama", "AL"),
            Map.entry("alaska", "AK"),
            Map.entry("arizona", "AZ"),
            Map.entry("arkansas", "AR"),
            Map.entry("california", "CA"),
            Map.entry("colorado", "CO"),
            Map.entry("connecticut", "CT"),
            Map.entry("district of columbia", "DC"),
            Map.entry("washington dc", "DC"),
            Map.entry("delaware", "DE"),
            Map.entry("florida", "FL"),
            Map.entry("georgia", "GA"),
            Map.entry("hawaii", "HI"),
            Map.entry("idaho", "ID"),
            Map.entry("illinois", "IL"),
            Map.entry("indiana", "IN"),
            Map.entry("iowa", "IA"),
            Map.entry("kansas", "KS"),
            Map.entry("kentucky", "KY"),
            Map.entry("louisiana", "LA"),
            Map.entry("maine", "ME"),
            Map.entry("maryland", "MD"),
            Map.entry("massachusetts", "MA"),
            Map.entry("michigan", "MI"),
            Map.entry("minnesota", "MN"),
            Map.entry("mississippi", "MS"),
            Map.entry("missouri", "MO"),
            Map.entry("montana", "MT"),
            Map.entry("nebraska", "NE"),
            Map.entry("nevada", "NV"),
            Map.entry("new hampshire", "NH"),
            Map.entry("new jersey", "NJ"),
            Map.entry("new mexico", "NM"),
            Map.entry("new york", "NY"),
            Map.entry("north carolina", "NC"),
            Map.entry("north dakota", "ND"),
            Map.entry("ohio", "OH"),
            Map.entry("oklahoma", "OK"),
            Map.entry("oregon", "OR"),
            Map.entry("pennsylvania", "PA"),
            Map.entry("rhode island", "RI"),
            Map.entry("south carolina", "SC"),
            Map.entry("south dakota", "SD"),
            Map.entry("tennessee", "TN"),
            Map.entry("texas", "TX"),
            Map.entry("utah", "UT"),
            Map.entry("vermont", "VT"),
            Map.entry("virginia", "VA"),
            Map.entry("washington", "WA"),
            Map.entry("west virginia", "WV"),
            Map.entry("wisconsin", "WI"),
            Map.entry("wyoming", "WY"),
            Map.entry("puerto rico", "PR"),
            Map.entry("guam", "GU"),
            Map.entry("us virgin islands", "VI"),
            Map.entry("u s virgin islands", "VI"),
            Map.entry("virgin islands", "VI"),
            Map.entry("american samoa", "AS"),
            Map.entry("northern mariana islands", "MP"),
            Map.entry("commonwealth of the northern mariana islands", "MP")
    );

    public VotingLocationsResponse getVotingLocations(String input) {
        String normalizedInput = input == null ? "" : input.trim();
        if (normalizedInput.isBlank()) {
            return new VotingLocationsResponse(
                    null,
                    "UNKNOWN",
                    "Provide your ZIP code for nearby options, or a full address for a best-match polling place lookup.",
                    List.of(),
                    resourcesFor(null, "UNKNOWN", null, normalizedInput)
            );
        }

        String stateCode = extractStateCode(normalizedInput);
        String zip = extractZip(normalizedInput);
        List<VotingLocation> locations = new ArrayList<>();
        String queryType = isZipOnly(normalizedInput) ? "ZIP" : "ADDRESS";
        ResolvedAddress resolvedAddress = resolveAddress(normalizedInput);
        if (stateCode == null && resolvedAddress != null && resolvedAddress.stateCode() != null) {
            stateCode = resolvedAddress.stateCode();
        }
        if (zip == null && resolvedAddress != null) {
            zip = resolvedAddress.zip();
        }
        if (stateCode == null && zip != null) {
            ZipLocality inferredFromZip = lookupZipLocality(zip);
            if (inferredFromZip != null) {
                stateCode = inferredFromZip.stateCode();
            }
        }

        List<VotingLocation> googleCivicLocations = fetchGoogleCivicLocations(normalizedInput);
        if (!googleCivicLocations.isEmpty()) {
            if (stateCode == null) {
                stateCode = inferStateFromLocations(googleCivicLocations);
            }
            return new VotingLocationsResponse(
                    stateCode,
                    queryType,
                    "Official locations from Google Civic Information API. Verify hours and rules with your election office.",
                    googleCivicLocations,
                    resourcesFor(stateCode, queryType, resolvedAddress, normalizedInput)
            );
        }

        if ("ADDRESS".equals(queryType) && "NC".equalsIgnoreCase(stateCode)) {
            VotingLocation ncOfficial = fetchNorthCarolinaOfficialLocation(resolvedAddress, normalizedInput);
            if (ncOfficial != null) {
                return new VotingLocationsResponse(
                        "NC",
                        queryType,
                        "Official assigned Election Day polling place from North Carolina State Board of Elections.",
                        List.of(ncOfficial),
                        resourcesFor("NC", queryType, resolvedAddress, normalizedInput)
                );
            }
        }

        if (queryType.equals("ZIP")) {
            ZipLocality locality = lookupZipLocality(zip);
            if (stateCode == null && locality != null) {
                stateCode = locality.stateCode();
            }
            String localityLabel = locality == null ? ("ZIP " + zip) : (locality.placeName() + ", " + locality.stateCode() + " " + zip);
            locations.addAll(fetchNominatimCivicLocations(localityLabel, zip, stateCode, 25, true));
        } else {
            locations.addAll(fetchNominatimCivicLocations(normalizedInput, null, stateCode, 6, false));
        }

        if (locations.isEmpty()) {
            GeoPoint center = geocode(normalizedInput);
            if (center != null && !center.label().isBlank()) {
                String label = center.label();
                locations.add(new VotingLocation(
                        "Voting lookup near your area",
                        label,
                        "No direct polling location records were returned, but you can start from this area and confirm with your election office.",
                        "OpenStreetMap geocoding",
                        googleMapsDirectionsUrl("polling place near " + label),
                        appleMapsUrl("polling place near " + label)
                ));
            }
        }

        List<ResourceOption> resources = resourcesFor(stateCode, queryType, resolvedAddress, normalizedInput);
        String note;
        if (!locations.isEmpty()) {
            note = "Showing nearby polling-related locations with printable addresses. Confirm your assigned location before Election Day.";
        } else if (queryType.equals("ZIP")) {
            note = "No direct polling locations were found for this ZIP right now. Use the official resources below to confirm your assigned site.";
        } else {
            note = "No direct polling locations were found for this address right now. Use the official resources below to confirm your assigned site.";
        }

        return new VotingLocationsResponse(stateCode, queryType, note, locations, resources);
    }

    private String extractStateCode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        Matcher matcher = STATE_CODE_WITH_ZIP.matcher(address);
        if (matcher.find()) {
            String candidate = matcher.group(1).toUpperCase(Locale.US);
            if (STATE_ELECTION_OFFICE_SITES.containsKey(candidate)) {
                return candidate;
            }
        }

        String[] tokens = address.split("[,\\s]+");
        for (String token : tokens) {
            String candidate = token.trim().toUpperCase(Locale.US);
            if (candidate.length() == 2 && STATE_ELECTION_OFFICE_SITES.containsKey(candidate)) {
                return candidate;
            }
        }

        String normalized = normalizeAlphaTokens(address);
        String matchedCode = null;
        int matchedLength = -1;
        for (Map.Entry<String, String> entry : JURISDICTION_NAME_TO_CODE.entrySet()) {
            String key = entry.getKey();
            if (normalized.contains(" " + key + " ") && key.length() > matchedLength) {
                matchedCode = entry.getValue();
                matchedLength = key.length();
            }
        }
        if (matchedCode != null && STATE_ELECTION_OFFICE_SITES.containsKey(matchedCode)) {
            return matchedCode;
        }
        return null;
    }

    private String extractZip(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        Matcher matcher = ZIP_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean isZipOnly(String input) {
        if (input == null) {
            return false;
        }
        return input.trim().matches("^\\d{5}(?:-\\d{4})?$");
    }

    private String googleMapsDirectionsUrl(String address) {
        return "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode(address, StandardCharsets.UTF_8);
    }

    private String appleMapsUrl(String address) {
        return "https://maps.apple.com/?q=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
    }

    private ZipLocality lookupZipLocality(String zip) {
        if (zip == null || zip.isBlank()) {
            return null;
        }
        try {
            String body = restTemplate.getForObject(ZIPPO_BASE + zip, String.class);
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode places = root.path("places");
            if (!places.isArray() || places.isEmpty()) {
                return null;
            }
            JsonNode firstPlace = places.get(0);
            String placeName = firstPlace.path("place name").asText("");
            String stateCode = firstPlace.path("state abbreviation").asText("");
            if (stateCode.length() != 2) {
                return null;
            }
            return new ZipLocality(placeName, stateCode.toUpperCase(Locale.US));
        } catch (Exception e) {
            return null;
        }
    }

    private List<VotingLocation> fetchGoogleCivicLocations(String input) {
        if (googleCivicApiKey == null || googleCivicApiKey.isBlank() || input == null || input.isBlank()) {
            return List.of();
        }
        try {
            String url = GOOGLE_CIVIC_BASE
                    + "?address=" + URLEncoder.encode(input, StandardCharsets.UTF_8)
                    + "&key=" + URLEncoder.encode(googleCivicApiKey, StandardCharsets.UTF_8);
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            List<VotingLocation> results = new ArrayList<>();
            appendGoogleCivicSites(results, root.path("pollingLocations"), "Polling location");
            appendGoogleCivicSites(results, root.path("earlyVoteSites"), "Early voting site");
            appendGoogleCivicSites(results, root.path("dropOffLocations"), "Ballot drop-off location");
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void appendGoogleCivicSites(List<VotingLocation> target, JsonNode sites, String fallbackName) {
        if (!sites.isArray()) {
            return;
        }
        for (JsonNode site : sites) {
            JsonNode address = site.path("address");
            String line1 = address.path("line1").asText("");
            String line2 = address.path("line2").asText("");
            String city = address.path("city").asText("");
            String state = address.path("state").asText("");
            String zip = address.path("zip").asText("");
            String fullAddress = joinNonBlank(line1, line2, city, state, zip);
            if (fullAddress.isBlank()) {
                continue;
            }
            String locationName = site.path("locationName").asText("").trim();
            if (locationName.isBlank()) {
                locationName = fallbackName;
            }
            target.add(new VotingLocation(
                    locationName,
                    fullAddress,
                    "Official polling information source.",
                    "Google Civic Information API",
                    googleMapsDirectionsUrl(fullAddress),
                    appleMapsUrl(fullAddress)
            ));
        }
    }

    private List<VotingLocation> fetchNominatimCivicLocations(
            String input,
            String zipHint,
            String expectedStateCode,
            int maxResults,
            boolean requireZipMatch
    ) {
        LinkedHashMap<String, VotingLocation> unique = new LinkedHashMap<>();
        List<String> queries = new ArrayList<>();
        String scopedInput = input + ", USA";
        queries.add("polling place near " + scopedInput);
        queries.add("board of elections near " + scopedInput);
        queries.add("county election office near " + scopedInput);
        if (zipHint != null) {
            queries.add("polling location " + zipHint + ", USA");
            queries.add("polling station " + zipHint + ", USA");
            queries.add("voting center " + zipHint + ", USA");
            queries.add("early voting site " + zipHint + ", USA");
        }

        int perQueryLimit = Math.max(5, Math.min(12, maxResults));
        for (String query : queries) {
            for (VotingLocation location : fetchNominatimSearchResults(
                    query,
                    perQueryLimit,
                    expectedStateCode,
                    zipHint,
                    requireZipMatch
            )) {
                String key = (location.name() + "|" + location.address()).toLowerCase(Locale.US);
                unique.putIfAbsent(key, location);
                if (unique.size() >= maxResults) {
                    return new ArrayList<>(unique.values());
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<VotingLocation> fetchNominatimSearchResults(
            String query,
            int limit,
            String expectedStateCode,
            String zipHint,
            boolean requireZipMatch
    ) {
        try {
            String url = NOMINATIM_SEARCH
                    + "?format=jsonv2&addressdetails=1&countrycodes=us&limit=" + limit
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CivicLens/1.0 (educational civics app)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray() || root.isEmpty()) {
                return List.of();
            }
            List<VotingLocation> results = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode addressNode = item.path("address");
                String resultStateCode = addressNode.path("state_code").asText("").toUpperCase(Locale.US);
                if (expectedStateCode != null && !expectedStateCode.isBlank() && !expectedStateCode.equalsIgnoreCase(resultStateCode)) {
                    continue;
                }
                String resultZip = extractZip(addressNode.path("postcode").asText(""));
                if (requireZipMatch && zipHint != null && (resultZip == null || !zipHint.equals(resultZip))) {
                    continue;
                }
                String name = item.path("name").asText("").trim();
                if (name.isBlank()) {
                    name = item.path("display_name").asText("").split(",")[0].trim();
                }
                if (name.isBlank()) {
                    name = "Voting location";
                }
                String addr = item.path("display_name").asText("").trim();
                if (addr.isBlank()) continue;

                results.add(new VotingLocation(
                        name,
                        addr,
                        "Location search result. Verify this site with your official local election office.",
                        "OpenStreetMap search",
                        googleMapsDirectionsUrl(addr),
                        appleMapsUrl(addr)
                ));
            }
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    private VotingLocation fetchNorthCarolinaOfficialLocation(ResolvedAddress resolvedAddress, String originalInput) {
        if (resolvedAddress == null) {
            return null;
        }
        String stateCode = resolvedAddress.stateCode();
        if (stateCode == null || !"NC".equalsIgnoreCase(stateCode)) {
            return null;
        }
        String street = resolvedAddress.street();
        String city = resolvedAddress.city();
        String zip = resolvedAddress.zip();
        if (street == null || street.isBlank() || city == null || city.isBlank() || zip == null || zip.isBlank()) {
            return null;
        }
        try {
            String url = NC_POLLING_LOOKUP
                    + "?Street=" + URLEncoder.encode(street, StandardCharsets.UTF_8)
                    + "&City=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&State=NC"
                    + "&Zip=" + URLEncoder.encode(zip, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CivicLens/1.0 (educational civics app)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            String html = response.getBody();
            if (!html.contains("Election Day Polling Place Information")) {
                return null;
            }

            Pattern blockPattern = Pattern.compile("Election Day Polling Place Information(.*?)View Images", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher blockMatcher = blockPattern.matcher(html);
            if (!blockMatcher.find()) {
                return null;
            }
            String block = blockMatcher.group(1);
            String normalizedBlock = block
                    .replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</p>", "\n")
                    .replaceAll("(?i)<[^>]+>", " ")
                    .replace("&nbsp;", " ")
                    .trim();
            if (normalizedBlock.isBlank()) {
                return null;
            }

            String name = "Assigned polling place";
            String address = "";
            String precinct = null;

            String[] rawParts = normalizedBlock.split("Precinct:");
            String left = rawParts[0].trim();
            if (rawParts.length > 1) {
                precinct = rawParts[1].trim();
            }

            String compactLeft = left.replaceAll("\\s+", " ").trim();
            Pattern linePattern = Pattern.compile("^(.+?)\\s+(\\d+\\s+.+)$");
            Matcher lineMatcher = linePattern.matcher(compactLeft);
            if (lineMatcher.find()) {
                name = lineMatcher.group(1).trim();
                address = lineMatcher.group(2).trim();
            } else {
                address = compactLeft;
            }

            if (address.isBlank()) {
                address = joinNonBlank(resolvedAddress.street(), resolvedAddress.city(), "NC", resolvedAddress.zip());
            }
            String description = precinct == null || precinct.isBlank()
                    ? "Official assigned Election Day polling place."
                    : "Official assigned Election Day polling place. Precinct: " + precinct;
            return new VotingLocation(
                    name.isBlank() ? "Assigned polling place" : name,
                    address,
                    description,
                    "NC State Board of Elections (official)",
                    googleMapsDirectionsUrl(address),
                    appleMapsUrl(address)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private GeoPoint geocode(String input) {
        ResolvedAddress resolvedAddress = resolveAddress(input);
        if (resolvedAddress == null) {
            return null;
        }
        return new GeoPoint(
                resolvedAddress.lat(),
                resolvedAddress.lon(),
                resolvedAddress.label()
        );
    }

    private ResolvedAddress resolveAddress(String input) {
        try {
            String url = NOMINATIM_SEARCH
                    + "?format=jsonv2&addressdetails=1&limit=1&q="
                    + URLEncoder.encode(input, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CivicLens/1.0 (educational civics app)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }
            JsonNode first = root.get(0);
            JsonNode address = first.path("address");
            String street = joinNonBlank(
                    address.path("house_number").asText(""),
                    firstNonBlank(
                            address.path("road").asText(""),
                            address.path("pedestrian").asText(""),
                            address.path("residential").asText("")
                    )
            );
            String city = firstNonBlank(
                    address.path("city").asText(""),
                    address.path("town").asText(""),
                    address.path("village").asText(""),
                    address.path("hamlet").asText(""),
                    address.path("municipality").asText(""),
                    address.path("county").asText("")
            );
            String stateCode = address.path("state_code").asText("").toUpperCase(Locale.US);
            if (stateCode.isBlank()) {
                stateCode = extractStateCode(input);
            }
            String zip = extractZip(address.path("postcode").asText(""));
            if (zip == null) {
                zip = extractZip(input);
            }
            String label = first.path("display_name").asText("");
            return new ResolvedAddress(
                    street,
                    city,
                    stateCode.isBlank() ? null : stateCode,
                    zip,
                    label,
                    first.path("lat").asDouble(),
                    first.path("lon").asDouble()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String inferStateFromLocations(List<VotingLocation> locations) {
        for (VotingLocation location : locations) {
            Matcher matcher = Pattern.compile("\\b([A-Z]{2})\\b").matcher(location.address());
            if (matcher.find()) {
                String candidate = matcher.group(1);
                if (STATE_ELECTION_OFFICE_SITES.containsKey(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String joinNonBlank(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private String firstNonBlank(String... parts) {
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                return part.trim();
            }
        }
        return "";
    }

    private String normalizeAlphaTokens(String value) {
        if (value == null || value.isBlank()) {
            return " ";
        }
        return " " + value.toLowerCase(Locale.US)
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";
    }

    private List<ResourceOption> defaultResources(String stateCode) {
        List<ResourceOption> resources = new ArrayList<>();
        String stateSite = stateCode == null ? null : STATE_ELECTION_OFFICE_SITES.get(stateCode);
        if (stateSite != null) {
            resources.add(new ResourceOption(
                    "State election office",
                    stateCode + " official voting information",
                    stateSite,
                    "Official source for polling place lookup, voting rules, and local election contacts."
            ));
        }
        resources.add(new ResourceOption(
                "U.S. Vote Foundation",
                "Find your election office",
                "https://www.usvotefoundation.org/election-offices",
                "Locate your local election office and verify polling place details."
        ));
        resources.add(new ResourceOption(
                "Vote.org polling place locator",
                "National polling place lookup",
                "https://www.vote.org/polling-place-locator/",
                "Another trusted way to confirm polling place and election requirements."
        ));
        resources.add(new ResourceOption(
                "Vote.gov",
                "Official U.S. voting portal",
                "https://www.vote.gov/",
                "Federal portal that routes voters to official state and territory election resources."
        ));
        return resources;
    }

    private List<ResourceOption> resourcesFor(
            String stateCode,
            String queryType,
            ResolvedAddress resolvedAddress,
            String rawInput
    ) {
        List<ResourceOption> resources = new ArrayList<>(defaultResources(stateCode));
        if (!"NC".equalsIgnoreCase(stateCode) || !"ADDRESS".equals(queryType)) {
            return resources;
        }
        String ncLookupUrl = northCarolinaLookupUrl(resolvedAddress);
        if (ncLookupUrl == null) {
            ncLookupUrl = northCarolinaLookupUrlFromInput(rawInput);
        }
        if (ncLookupUrl != null) {
            resources.add(0, new ResourceOption(
                    "NC State Board of Elections",
                    "North Carolina official polling place lookup",
                    ncLookupUrl,
                    "Official address-level lookup for your assigned Election Day polling place."
            ));
        }
        return resources;
    }

    private String northCarolinaLookupUrl(ResolvedAddress resolvedAddress) {
        if (resolvedAddress == null) {
            return null;
        }
        String street = resolvedAddress.street();
        String city = resolvedAddress.city();
        String zip = resolvedAddress.zip();
        if (street == null || street.isBlank() || city == null || city.isBlank() || zip == null || zip.isBlank()) {
            return null;
        }
        return NC_POLLING_LOOKUP
                + "?Street=" + URLEncoder.encode(street, StandardCharsets.UTF_8)
                + "&City=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "&State=NC"
                + "&Zip=" + URLEncoder.encode(zip, StandardCharsets.UTF_8);
    }

    private String northCarolinaLookupUrlFromInput(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }
        String state = extractStateCode(rawInput);
        if (!"NC".equalsIgnoreCase(state)) {
            return null;
        }
        String zip = extractZip(rawInput);
        if (zip == null) {
            return null;
        }
        String[] parts = rawInput.split(",");
        if (parts.length < 2) {
            return null;
        }
        String street = parts[0].trim();
        String city = parts[1].trim();
        if (street.isBlank() || city.isBlank()) {
            return null;
        }
        return NC_POLLING_LOOKUP
                + "?Street=" + URLEncoder.encode(street, StandardCharsets.UTF_8)
                + "&City=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "&State=NC"
                + "&Zip=" + URLEncoder.encode(zip, StandardCharsets.UTF_8);
    }

    public record VotingLocation(
            String name,
            String address,
            String description,
            String source,
            String googleMapsUrl,
            String appleMapsUrl
    ) {}

    public record ResourceOption(String provider, String title, String url, String description) {}

    public record VotingLocationsResponse(
            String stateCode,
            String queryType,
            String note,
            List<VotingLocation> locations,
            List<ResourceOption> resources
    ) {}

    private record ZipLocality(String placeName, String stateCode) {}
    private record GeoPoint(double lat, double lon, String label) {}
    private record ResolvedAddress(String street, String city, String stateCode, String zip, String label, double lat, double lon) {}
}
