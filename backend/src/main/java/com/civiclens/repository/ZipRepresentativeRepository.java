package com.civiclens.repository;

import com.civiclens.domain.Representative;
import com.civiclens.domain.ZipRepresentative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ZipRepresentativeRepository extends JpaRepository<ZipRepresentative, Long> {

    @Query("SELECT zr.representative FROM ZipRepresentative zr WHERE zr.zipCode = :zip")
    List<Representative> findRepresentativesByZipCode(String zip);

    boolean existsByZipCodeAndRepresentativeId(String zipCode, Long representativeId);
}
