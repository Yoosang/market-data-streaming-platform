package com.usang.marketdata.application.agent;

import com.usang.marketdata.application.agent.dto.CandleStats;
import com.usang.marketdata.application.agent.dto.NewsItem;
import com.usang.marketdata.application.agent.dto.WatchlistStat;
import com.usang.marketdata.application.alert.LatestPriceStore;
import com.usang.marketdata.application.news.NewsRetrievalService;
import com.usang.marketdata.application.surge.SurgeDetector;
import com.usang.marketdata.application.watchlist.WatchlistService;
import com.usang.marketdata.domain.candle.Candle;
import com.usang.marketdata.domain.candle.CandleRepository;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.infra.finnhub.FinnhubNewsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

// AI Agent(AgentChatService)가 호출하는 도구들의 실제 실행 로직.
// userId는 항상 메서드 파라미터로만 받고 도구의 JSON 스키마(Claude에게 노출되는 입력)에는
// 절대 포함하지 않는다 — Claude가 다른 사용자의 데이터를 요청할 수 없도록 구조적으로 차단.
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final WatchlistService watchlistService;
    private final LatestPriceStore latestPriceStore;
    private final SurgeDetector surgeDetector;
    private final NewsRetrievalService newsRetrievalService;
    private final FinnhubNewsClient finnhubNewsClient;
    private final CandleRepository candleRepository;

    public List<WatchlistStat> getWatchlist(String userId) {
        return watchlistService.getWatchlist(userId).stream()
                .map(this::toStat)
                .toList();
    }

    // US 종목: Finnhub 최근 헤드라인. KR 종목(숫자 코드): RAG로 수집된 뉴스 코퍼스에서 query와 유사도 검색
    // (AiBriefingService.fetchNewsHeadlines와 동일한 US/KR 분기 — 급등 감지 전용 흐름은 건드리지 않기 위해 재구현)
    public List<NewsItem> getRecentNews(String symbol, String query) {
        if (symbol.matches("\\d+")) {
            return newsRetrievalService.findRelevant(symbol, query, 3).stream()
                    .map(a -> new NewsItem(a.getTitle(), a.getDescription(), a.getUrl()))
                    .toList();
        }
        return finnhubNewsClient.fetchNews(symbol).stream()
                .map(n -> new NewsItem(n.headline(), n.summary(), n.url()))
                .toList();
    }

    // CandleController와 동일한 쿼리 재사용 — DESC로 조회해 최신 종가/가장 오래된 시가로 등락률 계산
    public CandleStats getCandleStats(String symbol, String interval, int count) {
        List<Candle> candles = candleRepository.findBySymbolAndIntervalTypeOrderByOpenTimeDesc(
                symbol, interval, PageRequest.of(0, count));
        if (candles.isEmpty()) {
            return new CandleStats(symbol, interval, 0, null, null, null, null);
        }

        double high = candles.stream().mapToDouble(Candle::getHigh).max().orElseThrow();
        double low = candles.stream().mapToDouble(Candle::getLow).min().orElseThrow();
        double latestClose = candles.get(0).getClose();
        double oldestOpen = candles.get(candles.size() - 1).getOpen();
        double changePercent = Math.round((latestClose - oldestOpen) / oldestOpen * 10000.0) / 100.0;

        return new CandleStats(symbol, interval, candles.size(), high, low, latestClose, changePercent);
    }

    private WatchlistStat toStat(Watchlist w) {
        Double currentPrice = latestPriceStore.getPrice(w.getSymbol()).orElse(null);
        double previousClose = surgeDetector.getPreviousClose(w.getSymbol());
        Double previousCloseOrNull = previousClose > 0 ? previousClose : null;
        Double changePercent = (currentPrice != null && previousCloseOrNull != null)
                ? Math.round((currentPrice - previousClose) / previousClose * 10000.0) / 100.0
                : null;
        return new WatchlistStat(w.getSymbol(), w.getMarket(), w.getName(),
                currentPrice, previousCloseOrNull, changePercent);
    }
}
