package com.usang.marketdata.application.news;

import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.infra.openai.OpenAiEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsRetrievalServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private OpenAiEmbeddingClient openAiEmbeddingClient;

    private NewsRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new NewsRetrievalService(newsArticleRepository, openAiEmbeddingClient);
    }

    @Test
    @DisplayName("코퍼스가 없으면 임베딩 호출 없이 빈 리스트를 반환한다")
    void 코퍼스_없으면_빈_리스트() {
        when(newsArticleRepository.existsBySymbol("005930")).thenReturn(false);

        List<NewsArticle> result = retrievalService.findRelevant("005930", "삼성전자 급등 이유", 3);

        assertThat(result).isEmpty();
        verifyNoInteractions(openAiEmbeddingClient);
    }

    @Test
    @DisplayName("코퍼스가 있으면 쿼리를 임베딩해 pgvector 유사도 검색 결과를 그대로 반환한다")
    void 코퍼스_있으면_임베딩_후_유사도검색_위임() {
        NewsArticle article = NewsArticle.of("005930", "삼성전자", "삼성전자 실적 서프라이즈", "설명",
                "https://a.com", LocalDateTime.now(), new float[]{0.1f, 0.2f});

        when(newsArticleRepository.existsBySymbol("005930")).thenReturn(true);
        float[] query = {1f, 0f};
        when(openAiEmbeddingClient.embed("쿼리")).thenReturn(query);
        when(newsArticleRepository.findTopKSimilar("005930", query, 2)).thenReturn(List.of(article));

        List<NewsArticle> result = retrievalService.findRelevant("005930", "쿼리", 2);

        assertThat(result).containsExactly(article);
    }
}
