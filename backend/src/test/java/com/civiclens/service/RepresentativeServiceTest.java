package com.civiclens.service;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Party;
import com.civiclens.domain.Representative;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.ZipRepresentativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepresentativeServiceTest {

    @Mock
    private ZipRepresentativeRepository zipRepresentativeRepository;
    @Mock
    private RepresentativeRepository representativeRepository;
    @Mock
    private RepresentativeEnrichmentService enrichmentService;

    @InjectMocks
    private RepresentativeService representativeService;

    @Test
    void getRepresentativesByZip_returnsListFromRepo() {
        when(enrichmentService.enrichFromApiIfNeeded("94110")).thenReturn(false);
        Representative rep = Representative.builder()
                .id(1L)
                .name("Jane Smith")
                .chamber(Chamber.HOUSE)
                .state("CA")
                .party(Party.DEMOCRATIC)
                .build();
        when(zipRepresentativeRepository.findRepresentativesByZipCode("94110")).thenReturn(List.of(rep));

        List<Representative> result = representativeService.getRepresentativesByZip("94110");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Jane Smith");
    }

    @Test
    void getRepresentativesByZip_returnsEmptyForBlankZip() {
        // enrichment not invoked for blank zip
        assertThat(representativeService.getRepresentativesByZip("")).isEmpty();
        assertThat(representativeService.getRepresentativesByZip("   ")).isEmpty();
        assertThat(representativeService.getRepresentativesByZip(null)).isEmpty();
    }

    @Test
    void getById_returnsRepresentativeWhenFound() {
        Representative rep = Representative.builder().id(1L).name("Jane").chamber(Chamber.HOUSE).state("CA").build();
        when(representativeRepository.findById(1L)).thenReturn(Optional.of(rep));

        assertThat(representativeService.getById(1L)).isEqualTo(rep);
    }

    @Test
    void getById_returnsNullWhenNotFound() {
        when(representativeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThat(representativeService.getById(999L)).isNull();
    }
}
