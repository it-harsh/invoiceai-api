package com.invoiceai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiChatService implements AiChatService {

    @Value("${app.ai.openai.api-key}")
    private String apiKey;

    @Value("${app.ai.openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String chat(String systemPrompt, List<ChatMessage> conversationHistory) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            for (ChatMessage msg : conversationHistory) {
                messages.add(Map.of(
                        "role", msg.role(),
                        "content", msg.content()
                ));
            }

            Map<String, Object> requestMap = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "messages", messages
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI Chat API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API returned status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            log.error("OpenAI chat failed", e);
            throw new RuntimeException("AI chat failed", e);
        }
    }
}
