package com.civiclens.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "representatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Representative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Chamber chamber;

    @Column(nullable = false, length = 2)
    private String state;

    private String district;

    @Enumerated(EnumType.STRING)
    private Party party;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "official_url")
    private String officialUrl;

    @OneToMany(mappedBy = "representative", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<DonorSummary> donorSummaries = new ArrayList<>();
}
