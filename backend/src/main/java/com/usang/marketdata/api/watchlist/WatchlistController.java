package com.usang.marketdata.api.watchlist;

import com.usang.marketdata.application.watchlist.WatchlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
                          @Valid @RequestBody AddSymbolRequest request) {
        watchlistService.addSymbol(userId, request.symbol(), request.market(), request.name());
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSymbol(@AuthenticationPrincipal String userId,
                             @PathVariable String symbol) {
        watchlistService.removeSymbol(userId, symbol);
    }

    // symbol: US 티커(알파벳) 또는 KR 종목코드(숫자) — 외부 API URL에 그대로 삽입되므로 형식을 여기서 제한
    record AddSymbolRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{1,10}$") String symbol,
            @NotBlank @Pattern(regexp = "^(US|KR)$") String market,
            String name) {}
    record WatchlistItem(String symbol, String market, String name) {}
}
