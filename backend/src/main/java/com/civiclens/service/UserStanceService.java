package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.civiclens.domain.Stance;
import com.civiclens.domain.User;
import com.civiclens.domain.UserStance;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.UserStanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStanceService {

    private final UserStanceRepository stanceRepository;
    private final RepresentativeRepository representativeRepository;

    @Transactional
    public UserStance setStanceOnRepresentative(Long userId, Long representativeId, Stance stance, String note) {
        Representative rep = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new IllegalArgumentException("Representative not found"));
        User user = new User();
        user.setId(userId);
        UserStance userStance = stanceRepository.findByUserIdAndRepresentativeId(userId, representativeId)
                .orElse(UserStance.builder()
                        .user(user)
                        .representative(rep)
                        .stance(stance)
                        .note(note)
                        .build());
        userStance.setStance(stance);
        userStance.setNote(note);
        return stanceRepository.save(userStance);
    }

    @Transactional(readOnly = true)
    public UserStance getStanceForRepresentative(Long userId, Long representativeId) {
        return stanceRepository.findByUserIdAndRepresentativeId(userId, representativeId).orElse(null);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserStance> getStancesForUser(Long userId) {
        return stanceRepository.findByUserId(userId);
    }
}
