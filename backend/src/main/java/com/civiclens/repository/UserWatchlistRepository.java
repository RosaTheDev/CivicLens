package com.civiclens.repository;

import com.civiclens.domain.UserWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWatchlistRepository extends JpaRepository<UserWatchlist, Long> {

    List<UserWatchlist> findByUserId(Long userId);

    Optional<UserWatchlist> findByUserIdAndRepresentativeId(Long userId, Long representativeId);

    boolean existsByUserIdAndRepresentativeId(Long userId, Long representativeId);

    void deleteByUserIdAndRepresentativeId(Long userId, Long representativeId);
}
