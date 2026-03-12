package com.civiclens.client;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CongressGovClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CongressGovClient client;

    @BeforeEach
    void setUp() {
        client = new CongressGovClient(restTemplate, new ObjectMapper());
    }

    private Representative sampleRep() {
        Representative rep = new Representative();
        rep.setId(1L);
        rep.setName("Jane Smith");
        rep.setState("CA");
        rep.setChamber(Chamber.HOUSE);
        return rep;
    }

    @Test
    void fetchRecentSponsoredBills_returnsEmptyWhenApiKeyMissing() {
        ReflectionTestUtils.setField(client, "apiKey", "");
        Representative rep = sampleRep();

        assertThat(client.fetchRecentSponsoredBills(rep, 3)).isEmpty();
    }

    @Test
    void fetchRecentSponsoredBills_returnsSummariesFromApi() throws Exception {
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "apiBaseUrl", "https://api.congress.gov/v3");

        Representative rep = sampleRep();

        String memberJson = """
            {
              "members": [
                {
                  "invertedOrderName": "Smith, Jane",
                  "state": "California",
                  "terms": { "item": { "chamber": "House of Representatives" } },
                  "bioguideId": "S000001"
                }
              ]
            }
            """;

        String sponsoredJson = """
            {
              "sponsoredLegislation": {
                "item": [
                  {
                    "latestTitle": "A test bill",
                    "number": "1",
                    "type": "HR",
                    "congress": "118",
                    "latestAction": { "text": "Passed the House" },
                    "url": "https://api.congress.gov/v3/bill"
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(any(URI.class), (Class<String>) any()))
                .thenReturn(memberJson)
                .thenReturn(sponsoredJson);

        List<CongressGovClient.BillSummary> result = client.fetchRecentSponsoredBills(rep, 3);

        assertThat(result).hasSize(1);
        CongressGovClient.BillSummary bill = result.get(0);
        assertThat(bill.title).isEqualTo("A test bill");
        assertThat(bill.description).contains("Passed the House");
        assertThat(bill.congressGovUrl).isEqualTo("https://www.congress.gov/bill/118th-congress/hr/1");
    }
}

