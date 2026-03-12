package com.civiclens.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zip_representatives", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"zip_code", "representative_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZipRepresentative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_id", nullable = false)
    private Representative representative;
}
