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
import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.infra.finnhub.FinnhubNewsClient;
import com.usang.marketdata.infra.finnhub.dto.FinnhubNewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentToolServiceTest {

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private LatestPriceStore latestPriceStore;

    @Mock
    private SurgeDetector surgeDetector;

    @Mock
    private NewsRetrievalService newsRetrievalService;

    @Mock
    private FinnhubNewsClient finnhubNewsClient;

    @Mock
    private CandleRepository candleRepository;

    private AgentToolService agentToolService;

    @BeforeEach
    void setUp() {
        agentToolService = new AgentToolService(watchlistService, latestPriceStore, surgeDetector,
                newsRetrievalService, finnhubNewsClient, candleRepository);
    }

    @Test
    @DisplayName("현재가와 전일종가가 모두 있으면 changePercent를 계산한다")
    void 현재가_전일종가_있으면_changePercent_계산() {
        when(watchlistService.getWatchlist("user1"))
                .thenReturn(List.of(Watchlist.of("user1", "AAPL", "US", null)));
        when(latestPriceStore.getPrice("AAPL")).thenReturn(Optional.of(110.0));
        when(surgeDetector.getPreviousClose("AAPL")).thenReturn(100.0);

        List<WatchlistStat> result = agentToolService.getWatchlist("user1");

        assertThat(result).containsExactly(
                new WatchlistStat("AAPL", "US", null, 110.0, 100.0, 10.0));
    }

    @Test
    @DisplayName("아직 시세를 받지 못한 종목은 currentPrice/changePercent가 null이다")
    void 시세_없으면_currentPrice_changePercent_null() {
        when(watchlistService.getWatchlist("user1"))
                .thenReturn(List.of(Watchlist.of("user1", "TSLA", "US", null)));
        when(latestPriceStore.getPrice("TSLA")).thenReturn(Optional.empty());
        when(surgeDetector.getPreviousClose("TSLA")).thenReturn(250.0);

        List<WatchlistStat> result = agentToolService.getWatchlist("user1");

        assertThat(result).containsExactly(
                new WatchlistStat("TSLA", "US", null, null, 250.0, null));
    }

    @Test
    @DisplayName("전일종가 조회에 실패하면(0) previousClose/changePercent가 null이다")
    void 전일종가_조회실패시_previousClose_changePercent_null() {
        when(watchlistService.getWatchlist("user1"))
                .thenReturn(List.of(Watchlist.of("user1", "005930", "KR", "삼성전자")));
        when(latestPriceStore.getPrice("005930")).thenReturn(Optional.of(70000.0));
        when(surgeDetector.getPreviousClose("005930")).thenReturn(0.0);

        List<WatchlistStat> result = agentToolService.getWatchlist("user1");

        assertThat(result).containsExactly(
                new WatchlistStat("005930", "KR", "삼성전자", 70000.0, null, null));
    }

    @Test
    @DisplayName("US 종목(알파벳)은 Finnhub 뉴스를 조회한다")
    void US_종목은_Finnhub_뉴스_조회() {
        when(finnhubNewsClient.fetchNews("AAPL")).thenReturn(List.of(
                new FinnhubNewsItem("Apple hits new high", "요약", "https://example.com/a", "Reuters")));

        List<NewsItem> result = agentToolService.getRecentNews("AAPL", "최근 실적 관련 뉴스");

        assertThat(result).containsExactly(
                new NewsItem("Apple hits new high", "요약", "https://example.com/a"));
        verifyNoInteractions(newsRetrievalService);
    }

    @Test
    @DisplayName("KR 종목(숫자 코드)은 RAG 코퍼스에서 query와 유사도 검색한다")
    void KR_종목은_RAG_유사도_검색() {
        NewsArticle article = NewsArticle.of("005930", "삼성전자", "삼성전자 실적 서프라이즈",
                "설명", "https://example.com/b", LocalDateTime.now(), null);
        when(newsRetrievalService.findRelevant(eq("005930"), anyString(), eq(3)))
                .thenReturn(List.of(article));

        List<NewsItem> result = agentToolService.getRecentNews("005930", "삼성전자 실적 관련 뉴스");

        assertThat(result).containsExactly(
                new NewsItem("삼성전자 실적 서프라이즈", "설명", "https://example.com/b"));
        verifyNoInteractions(finnhubNewsClient);
    }

    @Test
    @DisplayName("KR 종목 뉴스 코퍼스가 비어있으면 빈 목록을 반환한다")
    void KR_종목_코퍼스_비어있으면_빈_목록() {
        when(newsRetrievalService.findRelevant(eq("005930"), anyString(), eq(3))).thenReturn(List.of());

        List<NewsItem> result = agentToolService.getRecentNews("005930", "삼성전자 관련 뉴스");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("캔들이 있으면 최고가/최저가/최근종가/등락률을 계산한다")
    void 캔들_있으면_통계_계산() {
        // DESC 순서(최신이 먼저) — 최근종가는 candles.get(0), 등락률 기준 시가는 마지막 원소
        List<Candle> candles = List.of(
                Candle.of("AAPL", "1d", 108.0, 112.0, 107.0, 110.0, 1000, LocalDateTime.now()),
                Candle.of("AAPL", "1d", 100.0, 105.0, 98.0, 104.0, 1200, LocalDateTime.now().minusDays(1))
        );
        when(candleRepository.findBySymbolAndIntervalTypeOrderByOpenTimeDesc(eq("AAPL"), eq("1d"), any(Pageable.class)))
                .thenReturn(candles);

        CandleStats result = agentToolService.getCandleStats("AAPL", "1d", 5);

        assertThat(result).isEqualTo(new CandleStats("AAPL", "1d", 2, 112.0, 98.0, 110.0, 10.0));
    }

    @Test
    @DisplayName("캔들이 없으면 통계 필드가 모두 null이다")
    void 캔들_없으면_통계_null() {
        when(candleRepository.findBySymbolAndIntervalTypeOrderByOpenTimeDesc(eq("AAPL"), eq("1d"), any(Pageable.class)))
                .thenReturn(List.of());

        CandleStats result = agentToolService.getCandleStats("AAPL", "1d", 5);

        assertThat(result).isEqualTo(new CandleStats("AAPL", "1d", 0, null, null, null, null));
    }
}
