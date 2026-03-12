package com.civiclens.repository;

import com.civiclens.domain.UserStance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStanceRepository extends JpaRepository<UserStance, Long> {

    List<UserStance> findByUserId(Long userId);

    Optional<UserStance> findByUserIdAndRepresentativeId(Long userId, Long representativeId);
}
