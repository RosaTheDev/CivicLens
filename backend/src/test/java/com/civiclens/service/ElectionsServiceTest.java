package com.civiclens.service;

import com.civiclens.domain.Election;
import com.civiclens.domain.ElectionType;
import com.civiclens.repository.ElectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElectionsServiceTest {

    @Mock
    private ElectionRepository electionRepository;

    @InjectMocks
    private ElectionsService electionsService;

    @Test
    void getUpcomingElections_includesFederalAndNormalizedState() {
        LocalDate today = LocalDate.now();
        Election federal = Election.builder()
                .id(1L)
                .stateCode("US")
                .officeLevel("Federal")
                .title("Federal General Election")
                .electionType(ElectionType.GENERAL)
                .electionDate(today.plusDays(10))
                .description("Federal election")
                .build();
        Election state = Election.builder()
                .id(2L)
                .stateCode("NC")
                .officeLevel("State")
                .title("North Carolina Primary")
                .electionType(ElectionType.PRIMARY)
                .electionDate(today.plusDays(20))
                .description("State primary")
                .build();

        when(electionRepository.findByStateCodeInAndElectionDateGreaterThanEqualOrderByElectionDateAsc(
                List.of("US", "NC"), today))
                .thenReturn(List.of(federal, state));

        List<ElectionsService.ElectionDto> result = electionsService.getUpcomingElections("nc");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).stateCode()).isEqualTo("US");
        assertThat(result.get(0).daysUntil()).isEqualTo(10);
        assertThat(result.get(1).stateCode()).isEqualTo("NC");
        assertThat(result.get(1).electionType()).isEqualTo("PRIMARY");
    }

    @Test
    void getUpcomingElections_withInvalidStateFiltersToFederalOnly() {
        LocalDate today = LocalDate.now();
        when(electionRepository.findByStateCodeInAndElectionDateGreaterThanEqualOrderByElectionDateAsc(
                List.of("US"), today))
                .thenReturn(List.of());

        List<ElectionsService.ElectionDto> result = electionsService.getUpcomingElections("northcarolina");

        assertThat(result).isEmpty();
    }
}
