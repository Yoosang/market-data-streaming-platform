package com.usang.marketdata.infra.openai.dto;

import java.util.List;

public record OpenAiEmbeddingResponse(List<OpenAiEmbeddingItem> data) {}
