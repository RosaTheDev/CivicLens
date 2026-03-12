package com.civiclens.service;

import com.civiclens.client.CongressGovClient;
import com.civiclens.domain.Representative;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides recent bill links for a representative.
 *
 * Uses Congress.gov API when an API key is configured; otherwise falls back
 * to Congress.gov search URLs.
 */
@Service
@RequiredArgsConstructor
public class RecentBillsService {

    private final CongressGovClient congressGovClient;

    public List<RecentBill> getRecentBillsForRepresentative(Representative representative) {
        if (representative == null) {
            return List.of();
        }

        // 1) Try sponsored bills
        Set<RecentBill> bills = new LinkedHashSet<>();
        congressGovClient.fetchRecentSponsoredBills(representative, 3).forEach(s ->
                bills.add(new RecentBill(s.congressGovUrl, s.title, s.description)));

        // 2) If fewer than 3, supplement with cosponsored bills
        if (bills.size() < 3) {
            congressGovClient.fetchRecentCosponsoredBills(representative, 3 - bills.size()).forEach(s ->
                    bills.add(new RecentBill(s.congressGovUrl, s.title, s.description)));
        }

        // 3) If still empty (no API data), fall back to generic Congress.gov search cards
        if (bills.isEmpty()) {
            String name = representative.getName() != null ? representative.getName() : "Representative";
            String state = representative.getState() != null ? representative.getState() : "";
            String query = String.format("{\"source\":\"legislation\",\"search\":\"%s %s\"}", name, state).trim();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String baseUrl = "https://www.congress.gov/search?q=" + encodedQuery;
            bills.add(new RecentBill(
                    baseUrl + "&page=1",
                    "Recent legislation involving " + name,
                    "View recent bills and resolutions associated with " + name + " on Congress.gov."
            ));
            bills.add(new RecentBill(
                    baseUrl + "&page=2",
                    "Older legislation for " + name,
                    "Browse additional sponsored or cosponsored items on Congress.gov."
            ));
            bills.add(new RecentBill(
                    baseUrl + "&page=3",
                    "More activity for " + name,
                    "See more legislative activity connected to this member on Congress.gov."
            ));
        }

        return bills.stream().limit(3).toList();
    }

    public record RecentBill(String url, String title, String description) {}
}


