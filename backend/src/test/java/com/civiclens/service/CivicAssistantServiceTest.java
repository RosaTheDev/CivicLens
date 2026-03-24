package com.civiclens.service;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CivicAssistantServiceTest {

    @Mock
    private RepresentativeService representativeService;

    @Mock
    private RestTemplate restTemplate;

    private CivicAssistantService civicAssistantService;

    @BeforeEach
    void setUp() {
        civicAssistantService = new CivicAssistantService(representativeService, restTemplate, new ObjectMapper());
    }

    @Test
    void answer_returnsRepresentativesForZipQuestion() {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .state("NC")
                .chamber(Chamber.HOUSE)
                .build();
        when(representativeService.getRepresentativesByZip("28052")).thenReturn(List.of(rep));

        CivicAssistantService.AssistantAnswer answer =
                civicAssistantService.answer("Who are my representatives for 28052?", "nc");

        assertThat(answer.answer()).contains("For ZIP 28052");
        assertThat(answer.answer()).contains("Jane Smith");
        assertThat(answer.suggestedActions()).anyMatch(s -> s.contains("NC"));
        verify(representativeService).getRepresentativesByZip("28052");
    }

    @Test
    void answer_explainsDifferenceBetweenHouseAndSenate() {
        CivicAssistantService.AssistantAnswer answer =
                civicAssistantService.answer("What is the difference between house and senate?", null);

        assertThat(answer.answer()).contains("The House and Senate are the two chambers of Congress");
        assertThat(answer.suggestedActions()).isNotEmpty();
    }

    @Test
    void answer_usesLiveKnowledgeWhenAvailable() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(
                "{\"query\":{\"search\":[{\"title\":\"United States Congress\"}]}}"
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(
                        "{\"extract\":\"Sentence one. Sentence two. Sentence three. Sentence four.\"," +
                                "\"content_urls\":{\"desktop\":{\"page\":\"https://en.wikipedia.org/wiki/United_States_Congress\"}}}"
                ));

        CivicAssistantService.AssistantAnswer answer =
                civicAssistantService.answer("Explain federalism in US civics", null);

        assertThat(answer.answer()).isEqualTo("Sentence one. Sentence two. Sentence three.");
        assertThat(answer.suggestedActions()).anyMatch(s -> s.contains("Source: https://en.wikipedia.org/wiki/United_States_Congress"));
    }

    @Test
    void answer_fallsBackToBuiltInGuidanceWhenLiveKnowledgeFails() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("network unavailable"));

        CivicAssistantService.AssistantAnswer answer =
                civicAssistantService.answer("Tell me something complex about civics", null);

        assertThat(answer.answer()).contains("I can help with election basics and common civic questions");
        assertThat(answer.suggestedActions()).isNotEmpty();
    }
}
