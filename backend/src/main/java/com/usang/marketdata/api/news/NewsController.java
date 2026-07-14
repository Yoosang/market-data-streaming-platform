package com.usang.marketdata.api.news;

import com.usang.marketdata.application.news.NewsCollectionScheduler;
import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.finnhub.FinnhubNewsClient;
import com.usang.marketdata.infra.finnhub.dto.FinnhubNewsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 종목 상세 페이지의 뉴스 목록 — US는 Finnhub에서 매번 최신 조회, KR은 저장된 코퍼스 조회(없거나 새로고침 시 즉시 수집)
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final WatchlistRepository watchlistRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsCollectionScheduler newsCollectionScheduler;
    private final FinnhubNewsClient finnhubNewsClient;

    @GetMapping("/{symbol}")
    public List<NewsItemResponse> getNews(@AuthenticationPrincipal String userId,
                                           @PathVariable String symbol,
                                           @RequestParam(defaultValue = "false") boolean refresh) {
        if (!symbol.matches("\\d+")) {
            return finnhubNewsClient.fetchNews(symbol).stream()
                    .map(NewsItemResponse::fromFinnhub)
                    .toList();
        }

        List<NewsArticle> articles = newsArticleRepository.findTop5BySymbolOrderByPublishedAtDesc(symbol);
        if (refresh || articles.isEmpty()) {
            String companyName = watchlistRepository.findByUserIdAndSymbol(userId, symbol)
                    .map(Watchlist::getName)
                    .orElse(symbol);
            newsCollectionScheduler.collectForSymbol(symbol, companyName);
            articles = newsArticleRepository.findTop5BySymbolOrderByPublishedAtDesc(symbol);
        }

        return articles.stream().map(NewsItemResponse::fromArticle).toList();
    }

    record NewsItemResponse(String title, String description, String url, String source) {
        // 네이버 뉴스 검색 API는 언론사명을 제공하지 않아 KR은 source가 항상 null
        static NewsItemResponse fromArticle(NewsArticle article) {
            return new NewsItemResponse(article.getTitle(), article.getDescription(), article.getUrl(), null);
        }

        static NewsItemResponse fromFinnhub(FinnhubNewsItem item) {
            return new NewsItemResponse(item.headline(), item.summary(), item.url(), item.source());
        }
    }
}
