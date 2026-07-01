package com.usang.marketdata.application.watchlist;

import com.usang.marketdata.application.stock.SubscriptionManager;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final SubscriptionManager subscriptionManager;

    @Transactional(readOnly = true)
    public List<String> getSymbols(String userId) {
        return watchlistRepository.findByUserId(userId).stream()
                .map(Watchlist::getSymbol)
                .toList();
    }

    @Transactional
    public void addSymbol(String userId, String symbol) {
        // 이미 등록된 종목이면 조용히 무시 (멱등성 보장)
        if (watchlistRepository.findByUserIdAndSymbol(userId, symbol).isPresent()) {
            return;
        }
        watchlistRepository.save(Watchlist.of(userId, symbol));
        subscriptionManager.subscribe(symbol);
    }

    @Transactional
    public void removeSymbol(String userId, String symbol) {
        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
        // DB 삭제 후 다른 사용자가 구독 중인지 확인해서 필요시 Finnhub 구독 해제
        subscriptionManager.unsubscribe(symbol);
    }
}
