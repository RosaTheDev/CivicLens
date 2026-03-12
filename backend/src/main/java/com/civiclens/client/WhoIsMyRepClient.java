package com.civiclens.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Client for whoismyrepresentative.com API.
 * No API key required. Returns House member + both Senators for a given ZIP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhoIsMyRepClient {

    // Use HTTPS to avoid 301 HTML redirects from the API
    private static final String DEFAULT_BASE_URL = "https://whoismyrepresentative.com";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${civiclens.whoismyrep.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    /**
     * Fetch representatives (House + Senators) for a ZIP code.
     *
     * @param zip 5-digit (or 9-digit) ZIP code
     * @return list of API result DTOs, or empty list on error
     */
    public List<WhoIsMyRepResponse.Result> fetchByZip(String zip) {
        if (zip == null || zip.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = normalizedBase + "/getall_mems.php?zip=" + zip.trim() + "&output=json";
        try {
            // API returns JSON but with content-type text/html, so parse manually
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                return Collections.emptyList();
            }
            // If we somehow received an HTML redirect/error page, bail out gracefully
            if (body.trim().startsWith("<")) {
                log.warn("Who Is My Representative API returned HTML for zip {} at URL {}", zip, url);
                return Collections.emptyList();
            }
            WhoIsMyRepResponse response = objectMapper.readValue(body, WhoIsMyRepResponse.class);
            return response.getResults() != null ? response.getResults() : Collections.emptyList();
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Who Is My Representative API failed for zip {}: {}", zip, e.getMessage());
            return Collections.emptyList();
        }
    }
}
