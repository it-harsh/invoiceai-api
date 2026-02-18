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
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini", matchIfMissing = true)
@Slf4j
public class GeminiChatService implements AiChatService {

    @Value("${app.ai.gemini.api-key}")
    private String apiKey;

    @Value("${app.ai.gemini.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String chat(String systemPrompt, List<ChatMessage> conversationHistory) {
        try {
            List<Map<String, Object>> contents = new ArrayList<>();
            for (ChatMessage msg : conversationHistory) {
                String role = "assistant".equals(msg.role()) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.content()))
                ));
            }

            Map<String, Object> requestMap = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", contents
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini Chat API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API returned status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/candidates/0/content/parts/0/text").asText();
        } catch (Exception e) {
            log.error("Gemini chat failed", e);
            throw new RuntimeException("AI chat failed", e);
        }
    }
}
