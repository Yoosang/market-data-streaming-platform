package com.usang.marketdata.api.surge;

import com.usang.marketdata.application.alert.LatestPriceStore;
import com.usang.marketdata.application.surge.AiBriefingService;
import com.usang.marketdata.application.surge.SurgeDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 종목 상세 페이지의 "AI 분석" 버튼 — 급등 감지와 무관하게 온디맨드로 트리거
// 결과는 기존 AiBriefingService가 그대로 WebSocket AI_BRIEFING 메시지로 push (동기 응답 경로 없음)
@RestController
@RequestMapping("/api/ai-briefing")
@RequiredArgsConstructor
public class AiBriefingController {

    private final LatestPriceStore latestPriceStore;
    private final SurgeDetector surgeDetector;
    private final AiBriefingService aiBriefingService;

    @PostMapping("/{symbol}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void analyze(@PathVariable String symbol) {
        double currentPrice = latestPriceStore.getPrice(symbol)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "실시간 시세가 없어 분석할 수 없습니다"));

        double previousClose = surgeDetector.getPreviousClose(symbol);
        double changePercent = previousClose > 0
                ? (currentPrice - previousClose) / previousClose * 100.0
                : 0;
        String direction = changePercent >= 0 ? "UP" : "DOWN";

        aiBriefingService.generateAsync(symbol, changePercent, direction);
    }
}
