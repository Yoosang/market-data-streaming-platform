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

    // market별 종목 조회 — 각 WebSocket 클라이언트가 시작 시 자신이 담당할 종목만 구독하기 위해 사용
    List<Watchlist> findByMarket(String market);

    // 특정 종목을 관심종목으로 등록한 사용자 목록 — 시세/급등/AI 브리핑을 해당 사용자에게만 전송하기 위해 사용
    List<Watchlist> findBySymbol(String symbol);
}
