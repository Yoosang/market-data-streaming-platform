package com.usang.marketdata.domain.candle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    // 최근 N개 캔들 조회 — 차트 초기 로드 시 사용
    List<Candle> findBySymbolAndIntervalTypeOrderByOpenTimeDesc(String symbol, String intervalType);
}
