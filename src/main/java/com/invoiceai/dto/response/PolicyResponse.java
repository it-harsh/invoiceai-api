package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PolicyResponse {
    private UUID id;
    private String name;
    private String ruleType;
    private CategorySummary category;
    private BigDecimal thresholdAmount;
    private String requiredField;
    private boolean isActive;
    private Instant createdAt;

    @Getter
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
    }
}
