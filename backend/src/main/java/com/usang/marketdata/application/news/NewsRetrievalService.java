package com.usang.marketdata.application.news;

import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.infra.openai.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

// 종목별로 미리 수집·임베딩된 뉴스 코퍼스에서 쿼리와 가장 유사한 기사를 검색 (RAG의 Retrieval 단계)
@Service
@RequiredArgsConstructor
public class NewsRetrievalService {

    private final NewsArticleRepository newsArticleRepository;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;

    public List<NewsArticle> findRelevant(String symbol, String queryText, int topK) {
        List<NewsArticle> candidates = newsArticleRepository.findBySymbol(symbol);
        if (candidates.isEmpty()) return List.of();

        float[] queryEmbedding = openAiEmbeddingClient.embed(queryText);

        // 기사마다 유사도를 한 번씩만 계산한 뒤 정렬 — Comparator 안에서 매번 재계산하지 않도록
        return candidates.stream()
                .map(a -> new Scored(a, cosineSimilarity(queryEmbedding, openAiEmbeddingClient.deserialize(a.getEmbedding()))))
                .sorted(Comparator.comparingDouble(Scored::similarity).reversed())
                .limit(topK)
                .map(Scored::article)
                .toList();
    }

    private record Scored(NewsArticle article, double similarity) {}

    // 코사인 유사도: 두 벡터가 가리키는 방향이 얼마나 비슷한지 (1에 가까울수록 유사, -1이면 정반대)
    // 패키지 내부 테스트에서 직접 검증하기 위해 package-private
    static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
