package com.example.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OpenAiRequestTest {
    @Test
    void chatCompletionRequestExplicitlyDisablesStorage() {
        JsonNode body = new ObjectMapper().valueToTree(new OpenAiRequest("synthetic prompt"));

        assertThat(body.get("store").booleanValue()).isFalse();
        assertThat(body.get("model").asText()).isEqualTo(System.getenv("OPENAI_MODEL"));
        assertThat(body.get("temperature").doubleValue())
                .isEqualTo(Double.parseDouble(System.getenv("OPENAI_TEMPERATURE")));
        assertThat(body.get("max_tokens").intValue())
                .isEqualTo(Integer.parseInt(System.getenv("OPENAI_MAX_TOKENS")));
        assertThat(body.get("messages").get(0).get("content").asText()).isEqualTo("synthetic prompt");
        assertThat(body.has("Authorization")).isFalse();
    }
}
