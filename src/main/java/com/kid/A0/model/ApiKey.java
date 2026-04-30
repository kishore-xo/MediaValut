package com.kid.A0.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "api_key",indexes = {
        @Index(name = "idx_apikey_hashedKey",columnList = "hashed_Key")
})
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String hashedKey;
    @Column(nullable = false)
    private String prefix;
    @Column(nullable = false)
    private String name;
    @CurrentTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsed;
    private boolean revoked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
