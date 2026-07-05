package com.usang.marketdata.application.alert;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// 종목별 최신 체결가를 메모리에 보관 — DB 조회 없이 알림 조건을 빠르게 판단하기 위해 사용
@Component
public class LatestPriceStore {

    private final Map<String, Double> latestPrices = new ConcurrentHashMap<>();

    public void update(String symbol, double price) {
        latestPrices.put(symbol, price);
    }

    public Optional<Double> getPrice(String symbol) {
        return Optional.ofNullable(latestPrices.get(symbol));
    }
}
