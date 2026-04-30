package com.kid.A0.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(nullable = false)
    private BigDecimal monthlyPrice;
    @Column(nullable = false)
    private long rateLimitPerMinute;
    private boolean isActive;
    @Column(nullable = false)
    private int mediaCount;
//    @Column(nullable = false)
    private String photoSize;
//    @Column(nullable = false)
    private String videoSize;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions;
}
