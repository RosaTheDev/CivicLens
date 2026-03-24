package com.civiclens.client;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegislatorPhotoClientTest {

    @Mock
    private RestTemplate restTemplate;

    private LegislatorPhotoClient client;

    @BeforeEach
    void setUp() {
        client = new LegislatorPhotoClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "legislatorsCurrentUrl", "https://example.com/legislators-current.json");
        ReflectionTestUtils.setField(client, "legislatorsHistoricalUrl", "https://example.com/legislators-historical.json");
    }

    @Test
    void fetchMemberPhotoUrl_returnsPhotoForMatchingCurrentHouseMember() {
        LocalDate start = LocalDate.now().minusYears(1);
        LocalDate end = LocalDate.now().plusYears(1);

        String json = """
            [
              {
                "id": { "bioguide": "S000001" },
                "name": { "last": "Smith", "official_full": "Jane Smith" },
                "terms": [
                  { "type": "rep", "state": "CA", "district": 12, "start": "%s", "end": "%s" }
                ]
              }
            ]
            """.formatted(start, end);

        when(restTemplate.getForObject("https://example.com/legislators-current.json", String.class)).thenReturn(json);

        Representative rep = Representative.builder()
                .name("Jane Smith")
                .state("CA")
                .district("12")
                .chamber(Chamber.HOUSE)
                .build();

        String result = client.fetchMemberPhotoUrl(rep);
        assertThat(result).isEqualTo("https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/S000001.jpg");
    }

    @Test
    void fetchMemberPhotoUrl_fallsBackToHistoricalDatasetForFormerMember() {
        String currentJson = "[]";
        String historicalJson = """
            [
              {
                "id": { "bioguide": "B001135" },
                "name": { "last": "Burr", "official_full": "Richard Burr" },
                "terms": [
                  { "type": "sen", "state": "NC", "start": "2017-01-03", "end": "2023-01-03" }
                ]
              }
            ]
            """;

        when(restTemplate.getForObject("https://example.com/legislators-current.json", String.class)).thenReturn(currentJson);
        when(restTemplate.getForObject("https://example.com/legislators-historical.json", String.class)).thenReturn(historicalJson);

        Representative rep = Representative.builder()
                .name("Richard Burr")
                .state("NC")
                .chamber(Chamber.SENATE)
                .build();

        String result = client.fetchMemberPhotoUrl(rep);
        assertThat(result).isEqualTo("https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/B001135.jpg");
    }

    @Test
    void fetchMemberPhotoUrl_allowsHouseFallbackWhenDistrictIsStaleButNameMatches() {
        LocalDate start = LocalDate.now().minusYears(1);
        LocalDate end = LocalDate.now().plusYears(1);
        String currentJson = """
            [
              {
                "id": { "bioguide": "S001150" },
                "name": { "first": "Adam", "last": "Schiff", "official_full": "Adam Schiff" },
                "terms": [
                  { "type": "rep", "state": "CA", "district": 30, "start": "%s", "end": "%s" }
                ]
              }
            ]
            """.formatted(start, end);

        when(restTemplate.getForObject("https://example.com/legislators-current.json", String.class)).thenReturn(currentJson);

        Representative rep = Representative.builder()
                .name("Adam Schiff")
                .state("CA")
                .district("28")
                .chamber(Chamber.HOUSE)
                .build();

        String result = client.fetchMemberPhotoUrl(rep);
        assertThat(result).isEqualTo("https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/S001150.jpg");
    }

    @Test
    void fetchMemberPhotoUrl_allowsCrossChamberFallbackForStrongNameAndStateMatch() {
        LocalDate start = LocalDate.now().minusYears(1);
        LocalDate end = LocalDate.now().plusYears(1);
        String currentJson = """
            [
              {
                "id": { "bioguide": "S001150" },
                "name": { "first": "Adam B.", "last": "Schiff", "official_full": "Adam B. Schiff" },
                "terms": [
                  { "type": "sen", "state": "CA", "start": "%s", "end": "%s" }
                ]
              }
            ]
            """.formatted(start, end);

        when(restTemplate.getForObject("https://example.com/legislators-current.json", String.class)).thenReturn(currentJson);

        Representative rep = Representative.builder()
                .name("Adam Schiff")
                .state("CA")
                .district("28")
                .chamber(Chamber.HOUSE)
                .build();

        String result = client.fetchMemberPhotoUrl(rep);
        assertThat(result).isEqualTo("https://raw.githubusercontent.com/unitedstates/images/gh-pages/congress/225x275/S001150.jpg");
    }
}
