package com.usang.marketdata.application.watchlist;

import com.usang.marketdata.application.subscription.SubscriptionManager;
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
    public List<Watchlist> getWatchlist(String userId) {
        return watchlistRepository.findByUserId(userId);
    }

    @Transactional
    public void addSymbol(String userId, String symbol, String market) {
        if (watchlistRepository.findByUserIdAndSymbol(userId, symbol).isPresent()) {
            return;
        }
        watchlistRepository.save(Watchlist.of(userId, symbol, market));
        subscriptionManager.subscribe(symbol, market);
    }

    @Transactional
    public void removeSymbol(String userId, String symbol) {
        // 삭제 전에 market 정보를 먼저 조회해야 unsubscribe 라우팅이 가능
        Watchlist entry = watchlistRepository.findByUserIdAndSymbol(userId, symbol)
                .orElse(null);
        if (entry == null) return;

        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
        subscriptionManager.unsubscribe(symbol, entry.getMarket());
    }
}
