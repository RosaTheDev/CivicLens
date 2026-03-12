package com.civiclens.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_stances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_id")
    private Representative representative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stance stance;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void createdAt() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
