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
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "claude")
@Slf4j
public class ClaudeExtractionService implements AiExtractionService {

    @Value("${app.ai.claude.api-key}")
    private String apiKey;

    @Value("${app.ai.claude.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String EXTRACTION_PROMPT = """
            Extract invoice/receipt data from this image/document. Return ONLY valid JSON with this exact structure:
            {"vendorName":"Company Name","amount":123.45,"currency":"USD","taxAmount":10.00,"date":"2026-01-15","description":"Brief description","categoryHint":"one of: Office Supplies, Travel, Software & Subscriptions, Meals & Entertainment, Professional Services, Utilities, Marketing, Other","confidence":0.95,"lineItems":[{"description":"Item","quantity":1,"unitPrice":50.00,"total":50.00}]}
            If you cannot extract a field, use null.
            """;

    @Override
    public ExtractionResult extract(byte[] fileBytes, String fileType, String fileName) {
        try {
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            String mediaType = mapMediaType(fileType);

            String requestBody = objectMapper.writeValueAsString(new Object() {
                public final String model2 = model;
                public final int max_tokens = 4096;
                public final Object[] messages = new Object[]{
                    new Object() {
                        public final String role = "user";
                        public final Object[] content = new Object[]{
                            new Object() {
                                public final String type = "image";
                                public final Object source = new Object() {
                                    public final String type2 = "base64";
                                    public final String media_type = mediaType;
                                    public final String data = base64Data;
                                };
                            },
                            new Object() {
                                public final String type = "text";
                                public final String text = EXTRACTION_PROMPT;
                            }
                        };
                    }
                };
            });

            // Fix field name - Jackson serialization workaround
            requestBody = requestBody.replace("\"model2\"", "\"model\"").replace("\"type2\"", "\"type\"");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Claude API returned status " + response.statusCode());
            }

            return parseClaudeResponse(response.body());
        } catch (Exception e) {
            log.error("Failed to extract invoice data with Claude", e);
            throw new RuntimeException("AI extraction failed", e);
        }
    }

    private ExtractionResult parseClaudeResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.at("/content/0/text").asText();

        // Extract JSON from potential markdown code blocks
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            text = text.substring(0, text.indexOf("```"));
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            text = text.substring(0, text.indexOf("```"));
        }

        JsonNode data = objectMapper.readTree(text.trim());
        return buildResult(data, responseBody);
    }

    private String mapMediaType(String fileType) {
        return switch (fileType) {
            case "application/pdf" -> "application/pdf";
            case "image/png" -> "image/png";
            case "image/jpeg" -> "image/jpeg";
            case "image/webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private ExtractionResult buildResult(JsonNode data, String rawResponse) {
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
                .rawResponse(rawResponse)
                .build();
    }

    private String textOrNull(JsonNode n, String f) { JsonNode v = n.get(f); return v != null && !v.isNull() ? v.asText() : null; }
    private BigDecimal decimalOrNull(JsonNode n, String f) { JsonNode v = n.get(f); return v != null && !v.isNull() && v.isNumber() ? v.decimalValue() : null; }
    private LocalDate dateOrNull(JsonNode n, String f) { String v = textOrNull(n, f); if (v == null) return null; try { return LocalDate.parse(v); } catch (Exception e) { return null; } }
}
