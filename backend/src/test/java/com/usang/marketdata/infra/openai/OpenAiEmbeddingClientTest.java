package com.usang.marketdata.infra.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiEmbeddingClientTest {

    private final OpenAiEmbeddingClient client =
            new OpenAiEmbeddingClient(RestClient.create(), new ObjectMapper());

    @Test
    @DisplayName("OpenAI 임베딩 응답 JSON에서 float 배열을 추출한다")
    void 임베딩_응답_파싱() {
        String responseJson = """
                {
                  "object": "list",
                  "data": [
                    { "object": "embedding", "embedding": [0.1, -0.2, 0.35], "index": 0 }
                  ],
                  "model": "text-embedding-3-small"
                }
                """;

        float[] embedding = client.parseEmbedding(responseJson);

        assertThat(embedding).containsExactly(0.1f, -0.2f, 0.35f);
    }

    @Test
    @DisplayName("serialize/deserialize는 원본 벡터를 그대로 왕복 복원한다")
    void 직렬화_역직렬화_왕복() {
        float[] original = {0.123f, -0.456f, 0.789f};

        String json = client.serialize(original);
        float[] restored = client.deserialize(json);

        assertThat(restored).containsExactly(original);
    }
}
