package com.usang.marketdata.api.watchlist;

import com.usang.marketdata.application.watchlist.WatchlistService;
import com.usang.marketdata.domain.watchlist.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist/{userId}/symbols")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItem> getSymbols(@PathVariable String userId) {
        return watchlistService.getWatchlist(userId).stream()
                .map(w -> new WatchlistItem(w.getSymbol(), w.getMarket()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addSymbol(@PathVariable String userId, @RequestBody AddSymbolRequest request) {
        watchlistService.addSymbol(userId, request.symbol(), request.market());
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSymbol(@PathVariable String userId, @PathVariable String symbol) {
        watchlistService.removeSymbol(userId, symbol);
    }

    record AddSymbolRequest(String symbol, String market) {}

    record WatchlistItem(String symbol, String market) {}
}
