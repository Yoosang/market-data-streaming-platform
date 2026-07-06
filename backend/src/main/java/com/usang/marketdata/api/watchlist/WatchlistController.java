package com.usang.marketdata.api.watchlist;

import com.usang.marketdata.application.watchlist.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist/symbols")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItem> getSymbols(@AuthenticationPrincipal String userId) {
        return watchlistService.getWatchlist(userId).stream()
                .map(w -> new WatchlistItem(w.getSymbol(), w.getMarket(), w.getName()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addSymbol(@AuthenticationPrincipal String userId,
                          @RequestBody AddSymbolRequest request) {
        watchlistService.addSymbol(userId, request.symbol(), request.market(), request.name());
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSymbol(@AuthenticationPrincipal String userId,
                             @PathVariable String symbol) {
        watchlistService.removeSymbol(userId, symbol);
    }

    record AddSymbolRequest(String symbol, String market, String name) {}
    record WatchlistItem(String symbol, String market, String name) {}
}
