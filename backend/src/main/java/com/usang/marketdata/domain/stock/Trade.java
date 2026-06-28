package com.usang.marketdata.domain.stock;

public record Trade(
        String symbol,
        double price,
        double volume,
        long timestamp
) {}
