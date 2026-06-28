package com.usang.marketdata.infra.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Finnhub 단건 체결 데이터: {"s":"AAPL","p":189.50,"v":100,"t":1718000000000}
public record FinnhubTrade(
        @JsonProperty("s") String symbol,
        @JsonProperty("p") double price,
        @JsonProperty("v") double volume,
        @JsonProperty("t") long timestamp
) {}
