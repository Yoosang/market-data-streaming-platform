package com.usang.marketdata.application.news;

import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.naver.NaverNewsClient;
import com.usang.marketdata.infra.naver.dto.NaverNewsItem;
import com.usang.marketdata.infra.openai.OpenAiEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsCollectionSchedulerTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private NaverNewsClient naverNewsClient;

    @Mock
    private OpenAiEmbeddingClient openAiEmbeddingClient;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    private NewsCollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NewsCollectionScheduler(
                watchlistRepository, naverNewsClient, openAiEmbeddingClient, newsArticleRepository);
    }

    @Test
    @DisplayName("KR 관심종목이 없으면 뉴스 조회를 하지 않는다")
    void KR_관심종목_없으면_수집_스킵() {
        when(watchlistRepository.findByMarket("KR")).thenReturn(List.of());

        scheduler.collect();

        verifyNoInteractions(naverNewsClient);
        verifyNoInteractions(newsArticleRepository);
    }

    @Test
    @DisplayName("같은 종목을 여러 사용자가 담아도 종목당 한 번만 뉴스를 조회한다")
    void 같은_종목_중복_조회_방지() {
        when(watchlistRepository.findByMarket("KR")).thenReturn(List.of(
                Watchlist.of("user1", "005930", "KR", "삼성전자"),
                Watchlist.of("user2", "005930", "KR", "삼성전자")
        ));
        when(naverNewsClient.search("삼성전자")).thenReturn(List.of());

        scheduler.collect();

        verify(naverNewsClient, times(1)).search("삼성전자");
    }

    @Test
    @DisplayName("이미 저장된 URL의 기사는 다시 저장하지 않는다")
    void 중복_URL_기사는_스킵() {
        when(watchlistRepository.findByMarket("KR")).thenReturn(List.of(
                Watchlist.of("user1", "005930", "KR", "삼성전자")
        ));
        NaverNewsItem existing = new NaverNewsItem("제목", "https://already-saved.com",
                "설명", "Mon, 26 Aug 2019 09:20:00 +0900");
        when(naverNewsClient.search("삼성전자")).thenReturn(List.of(existing));
        when(newsArticleRepository.existsByUrl("https://already-saved.com")).thenReturn(true);

        scheduler.collect();

        verify(newsArticleRepository, never()).save(any());
        verifyNoInteractions(openAiEmbeddingClient);
    }

    @Test
    @DisplayName("신규 기사는 임베딩을 계산해 저장한다")
    void 신규_기사는_임베딩_후_저장() {
        when(watchlistRepository.findByMarket("KR")).thenReturn(List.of(
                Watchlist.of("user1", "005930", "KR", "삼성전자")
        ));
        NaverNewsItem newItem = new NaverNewsItem("삼성전자 실적 서프라이즈", "https://new-article.com",
                "반도체 호황", "Mon, 26 Aug 2019 09:20:00 +0900");
        when(naverNewsClient.search("삼성전자")).thenReturn(List.of(newItem));
        when(newsArticleRepository.existsByUrl("https://new-article.com")).thenReturn(false);
        float[] embedding = {0.1f, 0.2f};
        when(openAiEmbeddingClient.embed("삼성전자 실적 서프라이즈 반도체 호황")).thenReturn(embedding);

        scheduler.collect();

        verify(newsArticleRepository).save(argThat(article ->
                article.getSymbol().equals("005930")
                        && article.getUrl().equals("https://new-article.com")
                        && Arrays.equals(article.getEmbedding(), embedding)
        ));
    }

    @Test
    @DisplayName("RFC 1123 형식의 pubDate를 LocalDateTime으로 파싱한다")
    void pubDate_파싱() {
        LocalDateTime parsed = scheduler.parsePublishedAt("Mon, 26 Aug 2019 09:20:00 +0900");

        assertThat(parsed).isEqualTo(LocalDateTime.of(2019, 8, 26, 9, 20, 0));
    }

    @Test
    @DisplayName("pubDate 파싱에 실패하면 현재 시각으로 대체한다")
    void pubDate_파싱_실패시_현재시각_대체() {
        LocalDateTime before = LocalDateTime.now();

        LocalDateTime parsed = scheduler.parsePublishedAt("invalid-date");

        assertThat(parsed).isAfterOrEqualTo(before);
    }
}
