package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.civiclens.domain.Stance;
import com.civiclens.domain.UserStance;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.UserStanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStanceServiceTest {

    @Mock
    private UserStanceRepository stanceRepository;
    @Mock
    private RepresentativeRepository representativeRepository;

    @InjectMocks
    private UserStanceService userStanceService;

    @Test
    void setStanceOnRepresentative_createsNewStance() {
        Representative rep = new Representative();
        rep.setId(1L);
        when(representativeRepository.findById(1L)).thenReturn(Optional.of(rep));
        when(stanceRepository.findByUserIdAndRepresentativeId(10L, 1L)).thenReturn(Optional.empty());
        when(stanceRepository.save(any(UserStance.class))).thenAnswer(inv -> inv.getArgument(0));

        UserStance result = userStanceService.setStanceOnRepresentative(10L, 1L, Stance.SUPPORT, "Great rep");

        assertThat(result.getStance()).isEqualTo(Stance.SUPPORT);
        assertThat(result.getNote()).isEqualTo("Great rep");
        verify(stanceRepository).save(any(UserStance.class));
    }

    @Test
    void setStanceOnRepresentative_throwsWhenRepresentativeNotFound() {
        when(representativeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userStanceService.setStanceOnRepresentative(10L, 999L, Stance.SUPPORT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getStanceForRepresentative_returnsExistingStance() {
        UserStance stance = new UserStance();
        stance.setId(5L);
        stance.setStance(Stance.OPPOSE);
        when(stanceRepository.findByUserIdAndRepresentativeId(10L, 1L))
                .thenReturn(Optional.of(stance));

        UserStance result = userStanceService.getStanceForRepresentative(10L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getStance()).isEqualTo(Stance.OPPOSE);
    }

    @Test
    void getStancesForUser_returnsListFromRepository() {
        UserStance stance = new UserStance();
        stance.setId(5L);
        stance.setStance(Stance.SUPPORT);
        when(stanceRepository.findByUserId(10L)).thenReturn(List.of(stance));

        List<UserStance> result = userStanceService.getStancesForUser(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStance()).isEqualTo(Stance.SUPPORT);
    }
}
