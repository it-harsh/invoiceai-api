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
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini", matchIfMissing = true)
@Slf4j
public class GeminiExtractionService implements AiExtractionService {

    @Value("${app.ai.gemini.api-key}")
    private String apiKey;

    @Value("${app.ai.gemini.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String EXTRACTION_PROMPT = """
            Extract invoice/receipt data from this image/document. Return ONLY valid JSON with this exact structure:
            {
              "vendorName": "Company Name",
              "amount": 123.45,
              "currency": "USD",
              "taxAmount": 10.00,
              "date": "2026-01-15",
              "description": "Brief description of what was purchased",
              "categoryHint": "one of: Office Supplies, Travel, Software & Subscriptions, Meals & Entertainment, Professional Services, Utilities, Marketing, Other",
              "confidence": 0.95,
              "lineItems": [
                {"description": "Item name", "quantity": 1, "unitPrice": 50.00, "total": 50.00}
              ]
            }
            If you cannot extract a field, use null. For confidence, use 0.0-1.0 based on how certain you are.
            """;

    @Override
    public ExtractionResult extract(byte[] fileBytes, String fileType, String fileName) {
        try {
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);

            String mimeType = fileType;

            String requestBody = objectMapper.writeValueAsString(new Object() {
                public final Object[] contents = new Object[]{
                    new Object() {
                        public final Object[] parts = new Object[]{
                            new Object() {
                                public final String text = EXTRACTION_PROMPT;
                            },
                            new Object() {
                                public final Object inline_data = new Object() {
                                    public final String mime_type = mimeType;
                                    public final String data = base64Data;
                                };
                            }
                        };
                    }
                };
                public final Object generationConfig = new Object() {
                    public final String responseMimeType = "application/json";
                };
            });

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API returned status " + response.statusCode());
            }

            return parseGeminiResponse(response.body());
        } catch (Exception e) {
            log.error("Failed to extract invoice data with Gemini", e);
            throw new RuntimeException("AI extraction failed", e);
        }
    }

    private ExtractionResult parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.at("/candidates/0/content/parts/0/text").asText();

        JsonNode data = objectMapper.readTree(text);

        List<ExtractionResult.LineItem> lineItems = new ArrayList<>();
        JsonNode itemsNode = data.get("lineItems");
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                lineItems.add(ExtractionResult.LineItem.builder()
                        .description(getTextOrNull(item, "description"))
                        .quantity(getDecimalOrNull(item, "quantity"))
                        .unitPrice(getDecimalOrNull(item, "unitPrice"))
                        .total(getDecimalOrNull(item, "total"))
                        .build());
            }
        }

        return ExtractionResult.builder()
                .vendorName(getTextOrNull(data, "vendorName"))
                .amount(getDecimalOrNull(data, "amount"))
                .currency(getTextOrDefault(data, "currency", "USD"))
                .taxAmount(getDecimalOrNull(data, "taxAmount"))
                .date(getDateOrNull(data, "date"))
                .description(getTextOrNull(data, "description"))
                .categoryHint(getTextOrNull(data, "categoryHint"))
                .confidence(getDecimalOrDefault(data, "confidence", new BigDecimal("0.5")))
                .lineItems(lineItems)
                .rawResponse(responseBody)
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        String value = getTextOrNull(node, field);
        return value != null ? value : defaultValue;
    }

    private BigDecimal getDecimalOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull() && value.isNumber()) ? value.decimalValue() : null;
    }

    private BigDecimal getDecimalOrDefault(JsonNode node, String field, BigDecimal defaultValue) {
        BigDecimal value = getDecimalOrNull(node, field);
        return value != null ? value : defaultValue;
    }

    private LocalDate getDateOrNull(JsonNode node, String field) {
        String value = getTextOrNull(node, field);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
