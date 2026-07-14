package com.usang.marketdata.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 마이그레이션 도구가 없어 news_article 테이블/인덱스를 애플리케이션 시작 시 직접 보장 — 이미 있으면 아무 일도 하지 않음
@Component
@RequiredArgsConstructor
public class VectorSchemaInitializer {

    private final JdbcTemplate vectorJdbcTemplate;

    @PostConstruct
    void init() {
        vectorJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        vectorJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS news_article (
                    id BIGSERIAL PRIMARY KEY,
                    symbol VARCHAR(20) NOT NULL,
                    company_name VARCHAR(50) NOT NULL,
                    title VARCHAR(500) NOT NULL,
                    description VARCHAR(1000),
                    url VARCHAR(500) NOT NULL UNIQUE,
                    published_at TIMESTAMP,
                    embedding vector(1536),
                    created_at TIMESTAMP NOT NULL DEFAULT now()
                )
                """);
        vectorJdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS news_article_symbol_idx ON news_article (symbol)");
        vectorJdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS news_article_embedding_idx ON news_article
                USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)
                """);
    }
}
