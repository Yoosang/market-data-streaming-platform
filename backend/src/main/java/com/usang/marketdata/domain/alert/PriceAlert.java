package com.usang.marketdata.domain.alert;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_alert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private double targetPrice;

    // ABOVE: 현재가 >= 목표가, BELOW: 현재가 <= 목표가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertDirection direction;

    @Column(nullable = false)
    private boolean triggered = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static PriceAlert of(String userId, String symbol, double targetPrice, AlertDirection direction) {
        PriceAlert alert = new PriceAlert();
        alert.userId = userId;
        alert.symbol = symbol;
        alert.targetPrice = targetPrice;
        alert.direction = direction;
        return alert;
    }

    public void trigger() {
        this.triggered = true;
    }
}
