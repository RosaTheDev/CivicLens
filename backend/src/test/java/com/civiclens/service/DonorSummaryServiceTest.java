package com.civiclens.service;

import com.civiclens.domain.DonorSummary;
import com.civiclens.domain.Representative;
import com.civiclens.repository.DonorSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonorSummaryServiceTest {

    @Mock
    private DonorSummaryRepository donorSummaryRepository;

    @InjectMocks
    private DonorSummaryService donorSummaryService;

    @Test
    void getForRepresentative_returnsFirstWhenNoCycleYear() {
        Representative rep = new Representative();
        rep.setId(1L);
        DonorSummary summary = DonorSummary.builder()
                .id(1L)
                .representative(rep)
                .cycleYear(2024)
                .totalAmount(BigDecimal.valueOf(500000))
                .build();
        when(donorSummaryRepository.findByRepresentativeId(1L)).thenReturn(List.of(summary));

        DonorSummary result = donorSummaryService.getForRepresentative(1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
    }

    @Test
    void getForRepresentative_returnsByCycleYearWhenSpecified() {
        Representative rep = new Representative();
        rep.setId(1L);
        DonorSummary summary = DonorSummary.builder()
                .id(1L)
                .representative(rep)
                .cycleYear(2024)
                .build();
        when(donorSummaryRepository.findByRepresentativeIdAndCycleYear(1L, 2024)).thenReturn(Optional.of(summary));

        DonorSummary result = donorSummaryService.getForRepresentative(1L, 2024);

        assertThat(result).isNotNull();
        assertThat(result.getCycleYear()).isEqualTo(2024);
    }

    @Test
    void getForRepresentative_returnsNullWhenNotFound() {
        when(donorSummaryRepository.findByRepresentativeId(999L)).thenReturn(List.of());
        assertThat(donorSummaryService.getForRepresentative(999L, null)).isNull();
    }
}
