package com.usang.marketdata.infra.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Finnhub WebSocket 메시지 최상위 포맷
// type=trade 일 때만 data 배열에 체결 데이터가 담김
// type=ping 은 heartbeat (data 없음)
public record FinnhubMessage(
        @JsonProperty("type") String type,
        @JsonProperty("data") List<FinnhubTrade> data
) {}
