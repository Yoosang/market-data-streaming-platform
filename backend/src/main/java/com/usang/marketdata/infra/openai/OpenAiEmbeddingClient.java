package com.usang.marketdata.infra.openai;

import com.usang.marketdata.infra.openai.dto.OpenAiEmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

// OpenAI Embeddings API로 텍스트를 벡터로 변환 (RAG 코퍼스/쿼리 임베딩용)
// 벡터 DB 없이 MySQL TEXT 컬럼에 저장하므로 float[] <-> JSON 문자열 직렬화도 함께 담당
@Component
@Slf4j
public class OpenAiEmbeddingClient {

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.embedding-model}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public float[] embed(String text) {
        Map<String, Object> body = Map.of("model", model, "input", text);
        OpenAiEmbeddingResponse response = restClient.post()
                .uri(OPENAI_EMBEDDINGS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(OpenAiEmbeddingResponse.class);
        return toFloatArray(response);
    }

    // 패키지 내부 테스트에서 직접 검증하기 위해 package-private
    float[] toFloatArray(OpenAiEmbeddingResponse response) {
        List<Double> values = response.data().get(0).embedding();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }

    // NewsArticle.embedding(TEXT) 컬럼 저장용 직렬화
    public String serialize(float[] embedding) {
        return objectMapper.writeValueAsString(embedding);
    }

    // 저장된 embedding을 유사도 계산용으로 복원
    public float[] deserialize(String json) {
        return objectMapper.readValue(json, float[].class);
    }
}
