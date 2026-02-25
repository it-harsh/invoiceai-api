package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class VendorResponse {
    private UUID id;
    private String name;
    private CategorySummary defaultCategory;
    private int expenseCount;
    private BigDecimal totalAmount;
    private LocalDate lastExpenseDate;

    @Getter
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
        private String color;
    }
}
