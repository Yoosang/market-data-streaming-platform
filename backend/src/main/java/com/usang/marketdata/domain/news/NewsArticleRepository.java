package com.usang.marketdata.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // 배치 수집 시 이미 저장된 기사인지 중복 확인용
    boolean existsByUrl(String url);

    // 유사도 검색 대상 코퍼스 조회 (종목별)
    List<NewsArticle> findBySymbol(String symbol);
}
