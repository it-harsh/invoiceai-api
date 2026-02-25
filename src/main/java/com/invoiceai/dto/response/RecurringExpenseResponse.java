package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class RecurringExpenseResponse {
    private UUID id;
    private String vendorName;
    private BigDecimal amount;
    private String currency;
    private BigDecimal taxAmount;
    private String description;
    private CategorySummary category;
    private String frequency;
    private LocalDate nextDueDate;
    private Instant lastCreatedAt;
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
