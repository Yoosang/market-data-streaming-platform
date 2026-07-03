package com.usang.marketdata.infra.kis;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.application.broadcast.StockBroadcastService;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisWebsocketClientTest {

    @Mock private KisApprovalKeyProvider approvalKeyProvider;
    @Mock private StockBroadcastService stockBroadcastService;
    @Mock private WatchlistRepository watchlistRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private WebSocketSession session;

    private KisWebsocketClient client;

    // KIS pipe-delimited 샘플: 삼성전자 현재가 83,000원, 체결량 1,500주
    private static final String VALID_PAYLOAD =
            "0|H0UNCNT0|001|005930^153515^83000^5^100^0.12^82500^83000^83200^82800^82900^82700^1500";

    @BeforeEach
    void setUp() {
        client = new KisWebsocketClient(approvalKeyProvider, stockBroadcastService, watchlistRepository, objectMapper);
    }

    @Test
    @DisplayName("정상 H0UNCNT0 메시지가 들어오면 broadcast가 호출된다")
    void valid_trade_message_triggers_broadcast() throws Exception {
        client.handleTextMessage(session, new TextMessage(VALID_PAYLOAD));

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(stockBroadcastService).broadcast(captor.capture());

        Trade trade = captor.getValue();
        assertThat(trade.symbol()).isEqualTo("005930");
        assertThat(trade.price()).isEqualTo(83000.0);
        assertThat(trade.volume()).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("'|'가 없는 JSON 메시지(heartbeat 등)는 broadcast를 호출하지 않는다")
    void json_heartbeat_message_is_ignored() throws Exception {
        String heartbeat = "{\"header\":{\"tr_id\":\"PINGPONG\",\"datetime\":\"20240101120000\"}}";

        client.handleTextMessage(session, new TextMessage(heartbeat));

        verify(stockBroadcastService, never()).broadcast(any());
    }

    @Test
    @DisplayName("H0UNCNT0가 아닌 tr_id는 무시된다")
    void unknown_tr_id_is_ignored() throws Exception {
        String otherTrId = "0|H0STASP0|001|005930^...";

        client.handleTextMessage(session, new TextMessage(otherTrId));

        verify(stockBroadcastService, never()).broadcast(any());
    }

    @Test
    @DisplayName("데이터 파싱 실패 시 broadcast를 호출하지 않고 예외를 삼킨다")
    void parsing_error_does_not_propagate() throws Exception {
        String malformed = "0|H0UNCNT0|001|INVALID_DATA";

        // 예외 없이 메서드가 종료되어야 함
        client.handleTextMessage(session, new TextMessage(malformed));

        verify(stockBroadcastService, never()).broadcast(any());
    }
}
