package com.usang.marketdata.application.stock;

import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.finnhub.FinnhubWebsocketClient;
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
    private final WatchlistRepository watchlistRepository;

    // 현재 Finnhub에 구독 요청을 보낸 종목 집합 (thread-safe)
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();

    // 앱 시작 시 DB에 이미 저장된 관심종목을 추적 집합에 미리 반영
    // 실제 Finnhub 구독은 FinnhubWebsocketClient.afterConnectionEstablished()에서 처리
    @PostConstruct
    public void init() {
        watchlistRepository.findAll().stream()
                .map(w -> w.getSymbol())
                .distinct()
                .forEach(subscribedSymbols::add);
        log.info("SubscriptionManager initialized with symbols: {}", subscribedSymbols);
    }

    public void subscribe(String symbol) {
        // 이미 구독 중이면 Finnhub에 중복 요청하지 않음
        if (subscribedSymbols.add(symbol)) {
            finnhubWebsocketClient.subscribe(symbol);
            log.info("Subscribed to new symbol: {}", symbol);
        }
    }

    public void unsubscribe(String symbol) {
        // 다른 사용자가 아직 이 종목을 관심종목으로 갖고 있으면 구독 유지
        if (watchlistRepository.existsBySymbol(symbol)) {
            return;
        }
        if (subscribedSymbols.remove(symbol)) {
            finnhubWebsocketClient.unsubscribe(symbol);
            log.info("Unsubscribed from symbol: {}", symbol);
        }
    }
}
