// src/main/java/com/example/demo/ai/OpenAiClient.java
package com.example.demo.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class OpenAiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * 단일 프롬프트를 보내고, 첫 번째 choice의 content만 문자열로 반환.
     */
    public String chat(String prompt, double temperature) {
        ChatRequest.Message msg = new ChatRequest.Message("user", prompt);
        ChatRequest request = new ChatRequest(model, List.of(msg), temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatResponse> res =
                restTemplate.exchange(CHAT_URL, HttpMethod.POST, entity, ChatResponse.class);

        ChatResponse body = res.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }
        return body.getChoices().get(0).getMessage().getContent();
    }

    /* ====== 요청/응답 DTO ====== */

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            private String role;
            private String content;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private List<Choice> choices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private Message message;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            private String role;
            private String content;
        }
    }
}
