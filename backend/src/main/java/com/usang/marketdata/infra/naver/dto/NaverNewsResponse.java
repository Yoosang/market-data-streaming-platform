package com.usang.marketdata.infra.naver.dto;

import java.util.List;

public record NaverNewsResponse(List<NaverNewsItem> items) {}
