package com.usang.marketdata.infra.naver;

import com.usang.marketdata.infra.naver.dto.NaverNewsItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsClientTest {

    // 네이버 뉴스 검색 API 응답에 섞여오는 하이라이트 태그/HTML 엔티티 제거 로직 검증
    private final NaverNewsClient client = new NaverNewsClient(RestClient.create());

    @Test
    @DisplayName("검색어 하이라이트 <b> 태그와 HTML 엔티티를 제거한다")
    void 태그와_엔티티_제거() {
        NaverNewsItem raw = new NaverNewsItem(
                "<b>삼성전자</b> 3분기 실적 &quot;서프라이즈&quot;",
                "https://example.com/1",
                "<b>삼성전자</b>가 반도체 호황 &amp; 수요 증가에 힘입어...",
                "Mon, 26 Aug 2019 09:20:00 +0900"
        );

        NaverNewsItem cleaned = client.stripTags(raw);

        assertThat(cleaned.title()).isEqualTo("삼성전자 3분기 실적 \"서프라이즈\"");
        assertThat(cleaned.description()).doesNotContain("<b>", "</b>");
        assertThat(cleaned.link()).isEqualTo(raw.link());
        assertThat(cleaned.pubDate()).isEqualTo(raw.pubDate());
    }
}
