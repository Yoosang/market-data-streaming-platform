package com.usang.marketdata.domain.news;

import java.time.LocalDateTime;

// KR 종목 뉴스 기사 — RAG 코퍼스의 저장 단위. pgvector(Postgres)에 저장되며 JPA를 쓰지 않는 평범한 POJO
public class NewsArticle {

    private Long id;
    private final String symbol;
    private final String companyName;
    private final String title;
    private final String description;
    private final String url;
    private final LocalDateTime publishedAt;
    private final float[] embedding;
    private LocalDateTime createdAt;

    private NewsArticle(Long id, String symbol, String companyName, String title, String description,
                         String url, LocalDateTime publishedAt, float[] embedding, LocalDateTime createdAt) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.title = title;
        this.description = description;
        this.url = url;
        this.publishedAt = publishedAt;
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public static NewsArticle of(String symbol, String companyName, String title, String description,
                                  String url, LocalDateTime publishedAt, float[] embedding) {
        return new NewsArticle(null, symbol, companyName, title, description, url, publishedAt, embedding, null);
    }

    // DB row로부터 복원할 때 사용 — NewsArticleRepository의 RowMapper 전용
    public static NewsArticle reconstruct(Long id, String symbol, String companyName, String title,
                                           String description, String url, LocalDateTime publishedAt,
                                           float[] embedding, LocalDateTime createdAt) {
        return new NewsArticle(id, symbol, companyName, title, description, url, publishedAt, embedding, createdAt);
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public float[] getEmbedding() { return embedding; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
