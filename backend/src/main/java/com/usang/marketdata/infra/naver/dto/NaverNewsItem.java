package com.usang.marketdata.infra.naver.dto;

// 네이버 뉴스 검색 API 단건 응답. title/description은 검색어 하이라이트용 <b> 태그가 섞여 옴
public record NaverNewsItem(
        String title,
        String link,
        String description,
        String pubDate
) {}
