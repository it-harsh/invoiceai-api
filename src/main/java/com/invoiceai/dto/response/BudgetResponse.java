package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BudgetResponse {
    private UUID id;
    private CategorySummary category;
    private BigDecimal monthlyLimit;
    private boolean alertAt80;
    private boolean alertAt100;
    private boolean isActive;
    private Instant createdAt;

    @Getter
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
        private String color;
    }
}
