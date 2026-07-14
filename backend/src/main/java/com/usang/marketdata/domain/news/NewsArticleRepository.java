package com.usang.marketdata.domain.news;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// pgvector(Postgres) 전용 리포지토리 — JPA 대신 JdbcTemplate로 직접 SQL 실행
// (Hibernate의 vector 타입 매핑 없이, 벡터 리터럴을 문자열로 직접 주고받음)
@Repository
@RequiredArgsConstructor
public class NewsArticleRepository {

    private final JdbcTemplate vectorJdbcTemplate;

    private static final RowMapper<NewsArticle> ROW_MAPPER = (rs, rowNum) -> NewsArticle.reconstruct(
            rs.getLong("id"), rs.getString("symbol"), rs.getString("company_name"),
            rs.getString("title"), rs.getString("description"), rs.getString("url"),
            rs.getObject("published_at", LocalDateTime.class),
            parseVectorLiteral(rs.getString("embedding_text")),
            rs.getObject("created_at", LocalDateTime.class));

    private static final String SELECT_COLUMNS =
            "id, symbol, company_name, title, description, url, published_at, embedding::text AS embedding_text, created_at";

    // 배치 수집 시 이미 저장된 기사인지 중복 확인용
    public boolean existsByUrl(String url) {
        Boolean exists = vectorJdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM news_article WHERE url = ?)", Boolean.class, url);
        return Boolean.TRUE.equals(exists);
    }

    // 해당 종목에 코퍼스가 하나라도 있는지 — 없으면 임베딩 API 호출 자체를 건너뛰기 위한 저비용 확인용
    public boolean existsBySymbol(String symbol) {
        Boolean exists = vectorJdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM news_article WHERE symbol = ?)", Boolean.class, symbol);
        return Boolean.TRUE.equals(exists);
    }

    // 상세 페이지 뉴스 목록 조회용 — 최신순 상위 5건
    public List<NewsArticle> findTop5BySymbolOrderByPublishedAtDesc(String symbol) {
        return vectorJdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM news_article WHERE symbol = ? ORDER BY published_at DESC LIMIT 5",
                ROW_MAPPER, symbol);
    }

    // RAG 쿼리 구성 시 회사명 조회용 — 코퍼스가 있다면 항상 정확한 회사명을 가지고 있음
    public Optional<NewsArticle> findFirstBySymbol(String symbol) {
        return vectorJdbcTemplate.query(
                        "SELECT " + SELECT_COLUMNS + " FROM news_article WHERE symbol = ? LIMIT 1",
                        ROW_MAPPER, symbol)
                .stream().findFirst();
    }

    // 코사인 거리(<=>) 기준 상위 topK — pgvector가 DB에서 직접 유사도 정렬
    public List<NewsArticle> findTopKSimilar(String symbol, float[] queryEmbedding, int topK) {
        return vectorJdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM news_article WHERE symbol = ? "
                        + "ORDER BY embedding <=> ?::vector LIMIT ?",
                ROW_MAPPER, symbol, toVectorLiteral(queryEmbedding), topK);
    }

    public void save(NewsArticle article) {
        vectorJdbcTemplate.update(
                "INSERT INTO news_article "
                        + "(symbol, company_name, title, description, url, published_at, embedding, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?::vector, now())",
                article.getSymbol(), article.getCompanyName(), article.getTitle(), article.getDescription(),
                article.getUrl(), article.getPublishedAt(), toVectorLiteral(article.getEmbedding()));
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    private static float[] parseVectorLiteral(String text) {
        String[] parts = text.substring(1, text.length() - 1).split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
