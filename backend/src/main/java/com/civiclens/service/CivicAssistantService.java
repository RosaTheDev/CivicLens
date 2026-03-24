package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CivicAssistantService {

    private static final Pattern ZIP_PATTERN = Pattern.compile("\\b(\\d{5})(?:-\\d{4})?\\b");
    private final RepresentativeService representativeService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AssistantAnswer answer(String question, String stateHint) {
        String normalizedQuestion = question == null ? "" : question.trim();
        String lower = normalizedQuestion.toLowerCase(Locale.US);
        String normalizedState = normalizeState(stateHint);

        List<String> suggestions = new ArrayList<>();
        String response;

        if (containsAny(lower, "who are my representatives", "my representatives", "who represents me")) {
            String zip = extractZip(normalizedQuestion);
            if (zip != null) {
                List<Representative> reps = representativeService.getRepresentativesByZip(zip);
                if (!reps.isEmpty()) {
                    StringBuilder builder = new StringBuilder("For ZIP " + zip + ", your current representatives are:\n");
                    for (Representative rep : reps.stream().limit(4).toList()) {
                        String chamber = rep.getChamber() != null ? rep.getChamber().name() : "UNKNOWN";
                        builder.append("- ")
                                .append(rep.getName())
                                .append(" (")
                                .append(chamber)
                                .append(", ")
                                .append(rep.getState())
                                .append(")\n");
                    }
                    response = builder.toString().trim();
                    suggestions.add("Open Dashboard and search this ZIP to view full profiles.");
                    suggestions.add("Ask me: What does each chamber do differently?");
                } else {
                    response = "I could not find representatives for ZIP " + zip + " right now. Try searching the ZIP on Dashboard as a fallback.";
                    suggestions.add("Double-check the ZIP and try again.");
                }
            } else {
                response = "I can look that up, but I need a ZIP code. Try: 'Who are my representatives for 28052?'";
                suggestions.add("Ask: Who are my representatives for 94110?");
            }
        } else if (containsAny(lower, "house", "senate", "difference between house and senate")) {
            response = "The House and Senate are the two chambers of Congress. "
                    + "The House has 435 voting members and representation is based on each state's population, so larger states get more representatives. "
                    + "The Senate has 100 members total, with 2 senators per state regardless of population. "
                    + "House terms are 2 years; Senate terms are 6 years (staggered).";
            suggestions.add("Ask: Why do some states have more representatives?");
            suggestions.add("Ask: What does Congress do day-to-day?");
        } else if (containsAny(lower, "more representatives", "why do some states have more", "apportionment", "population")) {
            response = "House seats are apportioned by population after each U.S. Census. "
                    + "States with larger populations receive more seats in the House, while every state still keeps exactly 2 senators in the Senate. "
                    + "District boundaries are then drawn inside each state through redistricting.";
            suggestions.add("Ask: How is redistricting different from apportionment?");
        } else if (containsAny(lower, "what is congress", "congress")) {
            response = "Congress is the federal legislature of the United States, made up of the House of Representatives and the Senate. "
                    + "It writes and passes federal laws, approves budgets, conducts oversight, and confirms key appointments (Senate).";
            suggestions.add("Ask: Which chamber handles impeachment vs confirmations?");
        } else if (containsAny(lower, "primary")) {
            response = "Primary elections decide which candidate from each party advances to the general election. "
                    + "States run primaries differently: open, closed, or semi-open systems affect who can vote in each party's primary.";
            suggestions.add("Compare your state's primary date vs general election date.");
            suggestions.add("Check whether your state allows independent voters in primaries.");
        } else if (containsAny(lower, "general")) {
            response = "General elections are the final election where party nominees compete for office. "
                    + "For most federal offices, general elections happen in November of even-numbered years.";
            suggestions.add("Check countdown cards for upcoming federal and state elections.");
        } else if (containsAny(lower, "register", "deadline", "absentee", "mail ballot", "early voting")) {
            response = "Registration and ballot deadlines vary by state and election type. "
                    + "Always verify deadlines through your state election office, especially for absentee/mail ballot requests and early voting windows.";
            suggestions.add("Open your state election office link in Voting Locations.");
            suggestions.add("Use ZIP or address lookup to find local election office support.");
        } else if (containsAny(lower, "where", "poll", "vote", "location")) {
            response = "Use the Voting Locations section to search by ZIP (for nearby options) or full address (for a best-match polling lookup). "
                    + "Then verify with your official state or county election office, because polling places can change.";
            suggestions.add("Try Voting Locations with your full street address.");
        } else {
            LiveKnowledge live = fetchLiveCivicsKnowledge(normalizedQuestion);
            if (live != null) {
                response = live.answer();
                suggestions.add("Source: " + live.sourceUrl());
                suggestions.add("Ask a follow-up with more context, e.g. your state or ZIP.");
            } else {
                response = "I can help with election basics and common civic questions: "
                        + "House vs Senate, what Congress does, why representation differs by state population, "
                        + "primary vs general elections, deadlines, and how to verify polling locations.";
                suggestions.add("Ask: What is the difference between the House and Senate?");
                suggestions.add("Ask: Why do some states have more representatives than others?");
            }
        }

        if (normalizedState != null) {
            suggestions.add("Filter upcoming elections for " + normalizedState + ".");
            suggestions.add("Confirm " + normalizedState + " deadlines through the state election office link.");
        }

        return new AssistantAnswer(response, suggestions);
    }

    private boolean containsAny(String input, String... terms) {
        for (String term : terms) {
            if (input.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String extractZip(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = ZIP_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private LiveKnowledge fetchLiveCivicsKnowledge(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        try {
            String searchQuery = "United States civics " + question;
            String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srlimit=1&utf8=1&format=json&srsearch="
                    + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String searchBody = restTemplate.getForObject(searchUrl, String.class);
            if (searchBody == null || searchBody.isBlank()) {
                return null;
            }
            JsonNode searchRoot = objectMapper.readTree(searchBody);
            JsonNode searchResults = searchRoot.path("query").path("search");
            if (!searchResults.isArray() || searchResults.isEmpty()) {
                return null;
            }
            String title = searchResults.get(0).path("title").asText("");
            if (title.isBlank()) {
                return null;
            }

            String summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "CivicLens/1.0 (educational civics assistant)");
            ResponseEntity<String> summaryResp = restTemplate.exchange(summaryUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!summaryResp.getStatusCode().is2xxSuccessful() || summaryResp.getBody() == null) {
                return null;
            }

            JsonNode summaryRoot = objectMapper.readTree(summaryResp.getBody());
            String extract = summaryRoot.path("extract").asText("").trim();
            String sourceUrl = summaryRoot.path("content_urls").path("desktop").path("page").asText("https://en.wikipedia.org/wiki/" + title.replace(" ", "_"));
            if (extract.isBlank()) {
                return null;
            }
            return new LiveKnowledge(limitSentences(extract, 3), sourceUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private String limitSentences(String text, int maxSentences) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] parts = text.split("(?<=[.!?])\\s+");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (count > 0) builder.append(' ');
            builder.append(part.trim());
            count++;
            if (count >= maxSentences) break;
        }
        return builder.toString();
    }

    private record LiveKnowledge(String answer, String sourceUrl) {}

    private String normalizeState(String stateHint) {
        if (stateHint == null || stateHint.isBlank()) {
            return null;
        }
        String normalized = stateHint.trim().toUpperCase(Locale.US);
        return normalized.length() == 2 ? normalized : null;
    }

    public record AssistantAnswer(String answer, List<String> suggestedActions) {}
}
