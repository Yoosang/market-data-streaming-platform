package com.usang.marketdata.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // 배치 수집 시 이미 저장된 기사인지 중복 확인용
    boolean existsByUrl(String url);

    // 유사도 검색 대상 코퍼스 조회 (종목별)
    List<NewsArticle> findBySymbol(String symbol);

    // RAG 쿼리 구성 시 회사명 조회용 — 코퍼스가 있다면 항상 정확한 회사명을 가지고 있음
    Optional<NewsArticle> findFirstBySymbol(String symbol);
}
