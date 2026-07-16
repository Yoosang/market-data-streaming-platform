package com.usang.marketdata.application.agent.dto;

// get_candle_stats 도구의 결과 — 캔들이 하나도 없으면(수집 전 종목 등) 통계 필드는 모두 null
public record CandleStats(
        String symbol,
        String interval,
        int candleCount,
        Double high,
        Double low,
        Double latestClose,
        Double changePercent
) {}
