package com.usang.marketdata.infra.naver;

import com.usang.marketdata.infra.naver.dto.NaverNewsItem;
import com.usang.marketdata.infra.naver.dto.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// 네이버 뉴스 검색 API로 KR 종목 관련 뉴스를 조회 (RAG 코퍼스 수집용)
@Component
@RequiredArgsConstructor
@Slf4j
public class NaverNewsClient {

    private static final String NAVER_NEWS_URL = "https://openapi.naver.com/v1/search/news.json";

    private final RestClient restClient;

    @Value("${app.naver.client-id}")
    private String clientId;

    @Value("${app.naver.client-secret}")
    private String clientSecret;

    // query: 회사명(예: "삼성전자"). 최신순 10건 조회
    public List<NaverNewsItem> search(String query) {
        try {
            NaverNewsResponse response = restClient.get()
                    .uri(NAVER_NEWS_URL + "?query={query}&display=10&sort=date", query)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(NaverNewsResponse.class);

            if (response == null || response.items() == null) return List.of();
            return response.items().stream().map(this::stripTags).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch Naver news for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    // 검색어 하이라이트용 <b>, </b> 태그 및 &quot; 등 HTML 엔티티 제거
    NaverNewsItem stripTags(NaverNewsItem item) {
        return new NaverNewsItem(
                clean(item.title()),
                item.link(),
                clean(item.description()),
                item.pubDate()
        );
    }

    // 패키지 내부 테스트에서 직접 검증하기 위해 package-private
    String clean(String text) {
        if (text == null) return null;
        return text.replaceAll("</?b>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
