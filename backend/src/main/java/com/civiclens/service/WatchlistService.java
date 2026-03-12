package com.civiclens.service;

import com.civiclens.domain.Representative;
import com.civiclens.domain.User;
import com.civiclens.domain.UserWatchlist;
import org.hibernate.Hibernate;
import com.civiclens.repository.RepresentativeRepository;
import com.civiclens.repository.UserWatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final UserWatchlistRepository watchlistRepository;
    private final RepresentativeRepository representativeRepository;

    @Transactional
    public Representative addToWatchlist(Long userId, Long representativeId) {
        Representative rep = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new IllegalArgumentException("Representative not found"));
        if (watchlistRepository.existsByUserIdAndRepresentativeId(userId, representativeId)) {
            return rep;
        }
        User user = new User();
        user.setId(userId);
        watchlistRepository.save(UserWatchlist.builder()
                .user(user)
                .representative(rep)
                .build());
        return rep;
    }

    @Transactional
    public boolean removeFromWatchlist(Long userId, Long representativeId) {
        watchlistRepository.deleteByUserIdAndRepresentativeId(userId, representativeId);
        return true;
    }
    @Transactional(readOnly = true)
    public List<Representative> getWatchlist(Long userId) {
        return watchlistRepository.findByUserId(userId).stream()
                .map(uw -> {
                    Representative rep = uw.getRepresentative();
                    // Ensure the representative proxy is initialized before leaving the transaction
                    Hibernate.initialize(rep);
                    return rep;
                })
                .collect(Collectors.toList());
    }
}
