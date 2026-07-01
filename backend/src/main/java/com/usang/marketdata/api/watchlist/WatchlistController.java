package com.usang.marketdata.api.watchlist;

import com.usang.marketdata.application.watchlist.WatchlistService;
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
    public List<String> getSymbols(@PathVariable String userId) {
        return watchlistService.getSymbols(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addSymbol(@PathVariable String userId, @RequestBody AddSymbolRequest request) {
        watchlistService.addSymbol(userId, request.symbol());
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSymbol(@PathVariable String userId, @PathVariable String symbol) {
        watchlistService.removeSymbol(userId, symbol);
    }

    record AddSymbolRequest(String symbol) {}
}
