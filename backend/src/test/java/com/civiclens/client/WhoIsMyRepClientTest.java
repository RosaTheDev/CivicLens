package com.civiclens.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhoIsMyRepClientTest {

    @Mock
    private RestTemplate restTemplate;

    private WhoIsMyRepClient client;

    @BeforeEach
    void setUp() {
        client = new WhoIsMyRepClient(restTemplate, new ObjectMapper());
    }

    @Test
    void fetchByZip_returnsEmptyListForBlankZip() {
        assertThat(client.fetchByZip(null)).isEmpty();
        assertThat(client.fetchByZip("")).isEmpty();
        assertThat(client.fetchByZip("   ")).isEmpty();
    }

    @Test
    void fetchByZip_returnsResultsWhenApiRespondsWithJson() throws Exception {
        ReflectionTestUtils.setField(client, "baseUrl", "https://whoismyrepresentative.com");

        String json = """
            {
              "results": [
                { "name": "Jane Smith", "party": "Democrat", "state": "CA", "district": "12", "link": "https://example.com" }
              ]
            }
            """;

        when(restTemplate.getForObject("https://whoismyrepresentative.com/getall_mems.php?zip=94110&output=json", String.class))
                .thenReturn(json);

        List<WhoIsMyRepResponse.Result> results = client.fetchByZip("94110");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Jane Smith");
    }

    @Test
    void fetchByZip_returnsEmptyListWhenApiReturnsHtml() {
        ReflectionTestUtils.setField(client, "baseUrl", "https://whoismyrepresentative.com");

        String html = "<html><body>Moved</body></html>";
        when(restTemplate.getForObject("https://whoismyrepresentative.com/getall_mems.php?zip=94110&output=json", String.class))
                .thenReturn(html);

        List<WhoIsMyRepResponse.Result> results = client.fetchByZip("94110");

        assertThat(results).isEmpty();
    }
}

