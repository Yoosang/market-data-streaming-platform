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
import static org.assertj.core.api.Assertions.within;
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
    @DisplayName("동일한 방향의 벡터는 코사인 유사도가 1에 가깝다")
    void 동일_벡터_유사도_1() {
        float[] a = {1f, 2f, 3f};
        float[] b = {2f, 4f, 6f}; // a와 같은 방향, 크기만 다름

        double similarity = NewsRetrievalService.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(1.0, within(1e-6));
    }

    @Test
    @DisplayName("직교하는 벡터는 코사인 유사도가 0이다")
    void 직교_벡터_유사도_0() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};

        double similarity = NewsRetrievalService.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(0.0, within(1e-6));
    }

    @Test
    @DisplayName("정반대 방향 벡터는 코사인 유사도가 -1이다")
    void 정반대_벡터_유사도_음수1() {
        float[] a = {1f, 2f, 3f};
        float[] b = {-1f, -2f, -3f};

        double similarity = NewsRetrievalService.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(-1.0, within(1e-6));
    }

    @Test
    @DisplayName("저장된 기사가 없으면 임베딩 호출 없이 빈 리스트를 반환한다")
    void 코퍼스_없으면_빈_리스트() {
        when(newsArticleRepository.findBySymbol("005930")).thenReturn(List.of());

        List<NewsArticle> result = retrievalService.findRelevant("005930", "삼성전자 급등 이유", 3);

        assertThat(result).isEmpty();
        verifyNoInteractions(openAiEmbeddingClient);
    }

    @Test
    @DisplayName("코사인 유사도가 높은 순으로 topK개만 반환한다")
    void 유사도_상위_topK_반환() {
        NewsArticle low = NewsArticle.of("005930", "삼성전자", "낮은 유사도 기사", "설명",
                "https://a.com", LocalDateTime.now(), "low-json");
        NewsArticle high = NewsArticle.of("005930", "삼성전자", "높은 유사도 기사", "설명",
                "https://b.com", LocalDateTime.now(), "high-json");
        NewsArticle mid = NewsArticle.of("005930", "삼성전자", "중간 유사도 기사", "설명",
                "https://c.com", LocalDateTime.now(), "mid-json");

        when(newsArticleRepository.findBySymbol("005930")).thenReturn(List.of(low, high, mid));

        float[] query = {1f, 0f};
        when(openAiEmbeddingClient.embed("쿼리")).thenReturn(query);
        when(openAiEmbeddingClient.deserialize("low-json")).thenReturn(new float[]{0f, 1f});   // 유사도 0
        when(openAiEmbeddingClient.deserialize("high-json")).thenReturn(new float[]{1f, 0f});  // 유사도 1
        when(openAiEmbeddingClient.deserialize("mid-json")).thenReturn(new float[]{1f, 1f});   // 유사도 ~0.707

        List<NewsArticle> result = retrievalService.findRelevant("005930", "쿼리", 2);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(high, mid);
    }
}
