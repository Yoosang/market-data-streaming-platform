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

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    // "US" 또는 "KR" — 기존 데이터 보존을 위해 DB 레벨 DEFAULT 'US' 설정
    @Column(nullable = false, length = 5, columnDefinition = "VARCHAR(5) NOT NULL DEFAULT 'US'")
    private String market;

    // KR 종목명 (예: 삼성전자). US 종목은 null — 표시할 공식 한국어 이름이 없음
    @Column(length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static Watchlist of(String userId, String symbol, String market, String name) {
        Watchlist w = new Watchlist();
        w.userId = userId;
        w.symbol = symbol;
        w.market = market;
        w.name = name;
        w.createdAt = LocalDateTime.now();
        return w;
    }
}
