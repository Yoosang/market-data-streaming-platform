package com.usang.marketdata.domain.news;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// KR 종목 뉴스 기사 — RAG 코퍼스의 저장 단위. embedding은 Step2에서 채워짐(그 전까지 null)
@Entity
@Table(name = "news_article")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 50)
    private String companyName;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String description;

    // 동일 기사가 배치 수집마다 중복 저장되지 않도록 unique 제약
    @Column(nullable = false, unique = true, length = 500)
    private String url;

    private LocalDateTime publishedAt;

    // OpenAI 임베딩 결과(float[])를 JSON 문자열로 직렬화해 저장 — 벡터 DB 없이 MySQL만 사용
    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static NewsArticle of(String symbol, String companyName, String title,
                                  String description, String url, LocalDateTime publishedAt) {
        NewsArticle article = new NewsArticle();
        article.symbol = symbol;
        article.companyName = companyName;
        article.title = title;
        article.description = description;
        article.url = url;
        article.publishedAt = publishedAt;
        return article;
    }

    public void applyEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
