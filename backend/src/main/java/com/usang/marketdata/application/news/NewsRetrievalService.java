package com.usang.marketdata.application.news;

import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.news.NewsArticleRepository;
import com.usang.marketdata.infra.openai.OpenAiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 종목별로 미리 수집·임베딩된 뉴스 코퍼스에서 쿼리와 가장 유사한 기사를 검색 (RAG의 Retrieval 단계)
// 유사도 계산은 pgvector의 코사인 거리 연산자(<=>)로 DB에서 직접 수행
@Service
@RequiredArgsConstructor
public class NewsRetrievalService {

    private final NewsArticleRepository newsArticleRepository;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;

    public List<NewsArticle> findRelevant(String symbol, String queryText, int topK) {
        // 코퍼스가 없으면 임베딩 API 호출 자체를 건너뜀 (불필요한 비용 방지)
        if (!newsArticleRepository.existsBySymbol(symbol)) return List.of();

        float[] queryEmbedding = openAiEmbeddingClient.embed(queryText);
        return newsArticleRepository.findTopKSimilar(symbol, queryEmbedding, topK);
    }
}
