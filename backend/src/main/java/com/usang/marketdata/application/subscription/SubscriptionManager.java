package com.usang.marketdata.application.subscription;

import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.finnhub.FinnhubWebsocketClient;
import com.usang.marketdata.infra.kis.KisWebsocketClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionManager {

    private final FinnhubWebsocketClient finnhubWebsocketClient;
    private final KisWebsocketClient kisWebsocketClient;
    private final WatchlistRepository watchlistRepository;

    // 현재 구독 중인 종목 집합 (US/KR 구분 없이 심볼 문자열로 관리 — 두 시장 간 심볼 충돌 없음)
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        watchlistRepository.findAll().stream()
                .map(w -> w.getSymbol())
                .distinct()
                .forEach(subscribedSymbols::add);
        log.info("SubscriptionManager initialized with symbols: {}", subscribedSymbols);
    }

    public void subscribe(String symbol, String market) {
        if (subscribedSymbols.add(symbol)) {
            route(symbol, market, true);
            log.info("Subscribed {} [{}]", symbol, market);
        }
    }

    public void unsubscribe(String symbol, String market) {
        if (watchlistRepository.existsBySymbol(symbol)) {
            return; // 다른 사용자가 아직 구독 중
        }
        if (subscribedSymbols.remove(symbol)) {
            route(symbol, market, false);
            log.info("Unsubscribed {} [{}]", symbol, market);
        }
    }

    // market에 따라 Finnhub(US) 또는 KIS(KR) 클라이언트로 라우팅
    private void route(String symbol, String market, boolean subscribe) {
        if ("KR".equals(market)) {
            if (subscribe) kisWebsocketClient.subscribe(symbol);
            else kisWebsocketClient.unsubscribe(symbol);
        } else {
            if (subscribe) finnhubWebsocketClient.subscribe(symbol);
            else finnhubWebsocketClient.unsubscribe(symbol);
        }
    }
}
