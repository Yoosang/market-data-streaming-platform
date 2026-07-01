package com.usang.marketdata.domain.watchlist;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "watchlist",
    // 같은 사용자가 동일 종목을 중복 추가하지 못하도록 복합 유니크 제약
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static Watchlist of(String userId, String symbol) {
        Watchlist w = new Watchlist();
        w.userId = userId;
        w.symbol = symbol;
        w.createdAt = LocalDateTime.now();
        return w;
    }
}
