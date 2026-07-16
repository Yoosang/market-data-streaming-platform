package com.usang.marketdata.application.agent.dto;

// get_watchlist 도구의 결과 — currentPrice/previousClose/changePercent는 데이터가 없으면(예: 장 운영시간 외) null
public record WatchlistStat(
        String symbol,
        String market,
        String name,
        Double currentPrice,
        Double previousClose,
        Double changePercent
) {}
