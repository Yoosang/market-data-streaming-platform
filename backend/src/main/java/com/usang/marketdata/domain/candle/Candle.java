package com.usang.marketdata.domain.candle;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "candle",
    // 종목 + 집계 단위 + 시작 시각 조합이 고유함 (중복 저장 방지)
    uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "interval_type", "open_time"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    // "1m" 등 집계 단위 — 컬럼명을 interval_type으로 지정 (interval은 MySQL 예약어)
    @Column(name = "interval_type", nullable = false, length = 10)
    private String intervalType;

    @Column(nullable = false)
    private double open;

    @Column(nullable = false)
    private double high;

    @Column(nullable = false)
    private double low;

    @Column(nullable = false)
    private double close;

    @Column(nullable = false)
    private double volume;

    // 이 캔들이 시작된 분의 시각 (초/나노초는 0으로 잘라서 저장)
    @Column(name = "open_time", nullable = false)
    private LocalDateTime openTime;

    public static Candle of(String symbol, String intervalType,
                            double open, double high, double low, double close,
                            double volume, LocalDateTime openTime) {
        Candle c = new Candle();
        c.symbol = symbol;
        c.intervalType = intervalType;
        c.open = open;
        c.high = high;
        c.low = low;
        c.close = close;
        c.volume = volume;
        c.openTime = openTime;
        return c;
    }
}
