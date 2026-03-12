package com.civiclens.service;

import com.civiclens.domain.Chamber;
import com.civiclens.domain.Representative;
import com.civiclens.domain.User;
import com.civiclens.domain.UserWatchlist;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.UserWatchlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private UserWatchlistRepository watchlistRepository;
    @Mock
    private RepresentativeRepository representativeRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    private Representative rep;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rep = Representative.builder().id(1L).name("Jane").chamber(Chamber.HOUSE).state("CA").build();
    }

    @Test
    void addToWatchlist_savesAndReturnsRepresentative() {
        when(representativeRepository.findById(1L)).thenReturn(Optional.of(rep));
        when(watchlistRepository.existsByUserIdAndRepresentativeId(10L, 1L)).thenReturn(false);
        when(watchlistRepository.save(any(UserWatchlist.class))).thenAnswer(inv -> inv.getArgument(0));

        Representative result = watchlistService.addToWatchlist(10L, 1L);

        assertThat(result).isEqualTo(rep);
        ArgumentCaptor<UserWatchlist> captor = ArgumentCaptor.forClass(UserWatchlist.class);
        verify(watchlistRepository).save(captor.capture());
        assertThat(captor.getValue().getRepresentative().getId()).isEqualTo(1L);
    }

    @Test
    void addToWatchlist_throwsWhenRepresentativeNotFound() {
        when(representativeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.addToWatchlist(10L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void removeFromWatchlist_deletesAndReturnsTrue() {
        watchlistService.removeFromWatchlist(10L, 1L);
        verify(watchlistRepository).deleteByUserIdAndRepresentativeId(10L, 1L);
    }

    @Test
    void getWatchlist_returnsRepresentativesFromRepo() {
        UserWatchlist wl = new UserWatchlist();
        wl.setRepresentative(rep);
        when(watchlistRepository.findByUserId(10L)).thenReturn(List.of(wl));

        List<Representative> result = watchlistService.getWatchlist(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Jane");
    }
}
