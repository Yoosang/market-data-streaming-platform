package com.usang.marketdata.application.news;

import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.naver.NaverNewsClient;
import com.usang.marketdata.infra.naver.dto.NaverNewsItem;
import com.usang.marketdata.infra.openai.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// KR 관심종목의 뉴스를 주기적으로 수집해 임베딩 후 저장 — RAG 코퍼스를 미리 구축하는 배치 파이프라인
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsCollectionScheduler {

    private final WatchlistRepository watchlistRepository;
    private final NaverNewsClient naverNewsClient;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final NewsArticleRepository newsArticleRepository;

    // 30분마다 수집 — 네이버 뉴스 API 무료 쿼터(일 25,000건) 대비 관심종목 수를 고려한 주기
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void collect() {
        Map<String, String> symbolToName = watchlistRepository.findByMarket("KR").stream()
                // 같은 종목을 여러 사용자가 담아도 종목당 한 번만 수집
                .collect(Collectors.toMap(Watchlist::getSymbol, Watchlist::getName, (a, b) -> a));

        int saved = symbolToName.entrySet().stream()
                .mapToInt(entry -> collectForSymbol(entry.getKey(), entry.getValue()))
                .sum();
        log.info("News collection finished: {} symbol(s) scanned, {} new article(s) saved",
                symbolToName.size(), saved);
    }

    // 상세 페이지의 "뉴스 새로고침"에서 특정 종목만 즉시 수집할 때도 재사용
    public int collectForSymbol(String symbol, String companyName) {
        List<NaverNewsItem> items = naverNewsClient.search(companyName);
        int saved = 0;
        for (NaverNewsItem item : items) {
            if (newsArticleRepository.existsByUrl(item.link())) continue;

            try {
                float[] embedding = openAiEmbeddingClient.embed(item.title() + " " + item.description());
                NewsArticle article = NewsArticle.of(symbol, companyName, item.title(), item.description(),
                        item.link(), parsePublishedAt(item.pubDate()), embedding);
                newsArticleRepository.save(article);
                saved++;
            } catch (Exception e) {
                log.warn("Failed to save news article for {} ({}): {}", symbol, item.link(), e.getMessage());
            }
        }
        return saved;
    }

    // 패키지 내부 테스트에서 직접 검증하기 위해 package-private
    // 네이버 pubDate 포맷: "Mon, 26 Aug 2019 09:20:00 +0900" (RFC 1123)
    LocalDateTime parsePublishedAt(String pubDate) {
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse pubDate '{}': {}", pubDate, e.getMessage());
            return LocalDateTime.now();
        }
    }
}
