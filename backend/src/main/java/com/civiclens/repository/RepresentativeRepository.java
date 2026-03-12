package com.civiclens.repository;

import com.civiclens.domain.Representative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepresentativeRepository extends JpaRepository<Representative, Long> {

    Optional<Representative> findByExternalId(String externalId);
}
