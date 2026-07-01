package com.usang.marketdata.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserId(String userId);

    Optional<Watchlist> findByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);

    // 특정 종목을 관심종목으로 등록한 사용자가 한 명이라도 있는지 확인 (구독 해제 여부 결정용)
    boolean existsBySymbol(String symbol);
}
