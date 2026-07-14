package com.usang.marketdata.infra.openai;

import com.usang.marketdata.infra.openai.dto.OpenAiEmbeddingItem;
import com.usang.marketdata.infra.openai.dto.OpenAiEmbeddingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiEmbeddingClientTest {

    private final OpenAiEmbeddingClient client =
            new OpenAiEmbeddingClient(RestClient.create(), new ObjectMapper());

    @Test
    @DisplayName("OpenAI 임베딩 응답에서 float 배열을 추출한다")
    void 임베딩_응답_파싱() {
        OpenAiEmbeddingResponse response = new OpenAiEmbeddingResponse(
                List.of(new OpenAiEmbeddingItem(List.of(0.1, -0.2, 0.35))));

        float[] embedding = client.toFloatArray(response);

        assertThat(embedding).containsExactly(0.1f, -0.2f, 0.35f);
    }
}
