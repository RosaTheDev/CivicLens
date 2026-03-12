package com.civiclens.service;

import com.civiclens.client.CongressGovClient;
import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentBillsServiceTest {

    @Mock
    private CongressGovClient congressGovClient;

    @InjectMocks
    private RecentBillsService recentBillsService;

    @Test
    void getRecentBillsForRepresentative_returnsEmptyListWhenRepresentativeIsNull() {
        assertThat(recentBillsService.getRecentBillsForRepresentative(null)).isEmpty();
    }

    @Test
    void getRecentBillsForRepresentative_prefersSponsoredAndCosponsoredFromCongressGov() {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .build();

        CongressGovClient.BillSummary sponsored = new CongressGovClient.BillSummary(
                "Sponsored Bill", "Sponsored description", "https://congress.gov/bill/1");
        CongressGovClient.BillSummary cosponsored = new CongressGovClient.BillSummary(
                "Cosponsored Bill", "Cosponsored description", "https://congress.gov/bill/2");

        when(congressGovClient.fetchRecentSponsoredBills(rep, 3))
                .thenReturn(List.of(sponsored));
        when(congressGovClient.fetchRecentCosponsoredBills(rep, 2))
                .thenReturn(List.of(cosponsored));

        List<RecentBillsService.RecentBill> result = recentBillsService.getRecentBillsForRepresentative(rep);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Sponsored Bill");
        assertThat(result.get(1).title()).isEqualTo("Cosponsored Bill");
    }

    @Test
    void getRecentBillsForRepresentative_buildsFallbackSearchCardsWhenNoApiData() {
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .build();

        when(congressGovClient.fetchRecentSponsoredBills(rep, 3))
                .thenReturn(Collections.emptyList());
        when(congressGovClient.fetchRecentCosponsoredBills(rep, 3))
                .thenReturn(Collections.emptyList());

        List<RecentBillsService.RecentBill> result = recentBillsService.getRecentBillsForRepresentative(rep);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).title()).contains("Recent legislation involving Jane Smith");
        assertThat(result.get(0).url()).contains("https://www.congress.gov/search?q=");
    }
}

