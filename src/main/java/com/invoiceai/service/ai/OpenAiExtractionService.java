package com.invoiceai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiExtractionService implements AiExtractionService {

    @Value("${app.ai.openai.api-key}")
    private String apiKey;

    @Value("${app.ai.openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String EXTRACTION_PROMPT = """
            Extract invoice/receipt data from this image/document. Return ONLY valid JSON:
            {"vendorName":"Company Name","amount":123.45,"currency":"USD","taxAmount":10.00,"date":"2026-01-15","description":"Brief description","categoryHint":"one of: Office Supplies, Travel, Software & Subscriptions, Meals & Entertainment, Professional Services, Utilities, Marketing, Other","confidence":0.95,"lineItems":[{"description":"Item","quantity":1,"unitPrice":50.00,"total":50.00}]}
            If you cannot extract a field, use null.
            """;

    @Override
    public ExtractionResult extract(byte[] fileBytes, String fileType, String fileName) {
        try {
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            String dataUrl = "data:" + fileType + ";base64," + base64Data;

            String requestBody = """
                    {
                      "model": "%s",
                      "max_tokens": 4096,
                      "response_format": {"type": "json_object"},
                      "messages": [
                        {
                          "role": "user",
                          "content": [
                            {"type": "text", "text": "%s"},
                            {"type": "image_url", "image_url": {"url": "%s"}}
                          ]
                        }
                      ]
                    }
                    """.formatted(model, EXTRACTION_PROMPT.replace("\"", "\\\"").replace("\n", "\\n"), dataUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("OpenAI API returned status " + response.statusCode());
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            log.error("Failed to extract invoice data with OpenAI", e);
            throw new RuntimeException("AI extraction failed", e);
        }
    }

    private ExtractionResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.at("/choices/0/message/content").asText();
        JsonNode data = objectMapper.readTree(text);

        List<ExtractionResult.LineItem> lineItems = new ArrayList<>();
        JsonNode itemsNode = data.get("lineItems");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                lineItems.add(ExtractionResult.LineItem.builder()
                        .description(textOrNull(item, "description"))
                        .quantity(decimalOrNull(item, "quantity"))
                        .unitPrice(decimalOrNull(item, "unitPrice"))
                        .total(decimalOrNull(item, "total"))
                        .build());
            }
        }

        return ExtractionResult.builder()
                .vendorName(textOrNull(data, "vendorName"))
                .amount(decimalOrNull(data, "amount"))
                .currency(data.has("currency") && !data.get("currency").isNull() ? data.get("currency").asText() : "USD")
                .taxAmount(decimalOrNull(data, "taxAmount"))
                .date(dateOrNull(data, "date"))
                .description(textOrNull(data, "description"))
                .categoryHint(textOrNull(data, "categoryHint"))
                .confidence(data.has("confidence") && !data.get("confidence").isNull() ? data.get("confidence").decimalValue() : new BigDecimal("0.5"))
                .lineItems(lineItems)
                .rawResponse(responseBody)
                .build();
    }

    private String textOrNull(JsonNode n, String f) { JsonNode v = n.get(f); return v != null && !v.isNull() ? v.asText() : null; }
    private BigDecimal decimalOrNull(JsonNode n, String f) { JsonNode v = n.get(f); return v != null && !v.isNull() && v.isNumber() ? v.decimalValue() : null; }
    private LocalDate dateOrNull(JsonNode n, String f) { String v = textOrNull(n, f); if (v == null) return null; try { return LocalDate.parse(v); } catch (Exception e) { return null; } }
}
