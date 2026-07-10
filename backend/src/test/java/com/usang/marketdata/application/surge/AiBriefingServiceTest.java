package com.usang.marketdata.application.surge;

import com.usang.marketdata.api.stock.StockWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiBriefingServiceTest {

    @Mock
    private StockWebSocketHandler stockWebSocketHandler;

    // RestClient 생성자 주입 — 실제 HTTP 호출 대신 mock으로 교체
    @Mock
    private RestClient restClient;

    private AiBriefingService aiBriefingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiBriefingService = new AiBriefingService(stockWebSocketHandler, objectMapper, restClient);
        // @Value 필드는 Spring 컨텍스트 없이는 주입되지 않아 ReflectionTestUtils로 설정
        ReflectionTestUtils.setField(aiBriefingService, "anthropicApiKey", "test-api-key");
        ReflectionTestUtils.setField(aiBriefingService, "model", "claude-test");
        ReflectionTestUtils.setField(aiBriefingService, "finnhubToken", "test-token");
    }

    @Test
    @DisplayName("Claude API 호출 실패 시 fallback 메시지가 WebSocket으로 전송된다")
    void API_호출_실패시_fallback_메시지_전송() throws Exception {
        // given: Claude API POST 호출이 실패하는 상황 (타임아웃, 서버 오류 등)
        when(restClient.post()).thenThrow(new RuntimeException("Connection timeout"));

        // when: KR 종목은 뉴스 조회(GET)를 건너뛰어 POST만 실패
        // @Async는 Spring 컨텍스트 없이 단위 테스트 시 동기 실행됨
        aiBriefingService.generateAsync("005930", -5.5, "DOWN");

        // then: fallback 메시지가 전송됨
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stockWebSocketHandler).sendToAll(captor.capture());

        String message = captor.getValue();
        assertThat(message).contains("\"type\":\"AI_BRIEFING\"");
        assertThat(message).contains("\"symbol\":\"005930\"");
        assertThat(message).contains("일시적으로 제공할 수 없습니다");
    }

    @Test
    @DisplayName("Claude API가 1회 실패해도 retry 후 실패 시 fallback이 전송된다")
    void API_재시도_후_최종_실패시_fallback_전송() {
        // given: 1회 retry 포함 두 번 모두 실패
        when(restClient.post()).thenThrow(new RuntimeException("Server Error"));

        // when
        aiBriefingService.generateAsync("005930", 6.0, "UP");

        // then: POST가 2번 시도(1번 + 1번 retry)되었음 확인
        verify(restClient, times(2)).post();
        // fallback 메시지 전송
        verify(stockWebSocketHandler).sendToAll(contains("일시적으로 제공할 수 없습니다"));
    }

    @Test
    @DisplayName("KR 종목(숫자 코드)은 Finnhub 뉴스 API를 호출하지 않는다")
    void KR_종목은_뉴스_API_미호출() {
        // given: POST 실패 (뉴스 호출 여부만 검증하기 위해 빠르게 종료)
        when(restClient.post()).thenThrow(new RuntimeException("fail"));

        // when: 국내 종목 코드 (숫자)
        aiBriefingService.generateAsync("005930", 5.1, "UP");

        // then: 뉴스 조회용 GET은 호출되지 않음
        verify(restClient, never()).get();
    }

    @Test
    @DisplayName("AI_BRIEFING 메시지는 symbol, briefing, newsCount 필드를 포함한다")
    void fallback_메시지_포맷_검증() throws Exception {
        // given
        when(restClient.post()).thenThrow(new RuntimeException("fail"));

        // when
        aiBriefingService.generateAsync("005930", -7.2, "DOWN");

        // then: 메시지 포맷 검증
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stockWebSocketHandler).sendToAll(captor.capture());

        String message = captor.getValue();
        assertThat(message).contains("\"type\":\"AI_BRIEFING\"");
        assertThat(message).contains("\"symbol\":\"005930\"");
        assertThat(message).contains("\"newsCount\":0"); // fallback은 newsCount 0
        assertThat(message).contains("\"briefing\":");
    }
}
