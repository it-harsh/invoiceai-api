package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class BudgetProgressResponse {
    private BudgetItem overall;
    private List<BudgetItem> byCategory;

    @Getter
    @Builder
    public static class BudgetItem {
        private UUID budgetId;
        private String categoryName;
        private String categoryColor;
        private BigDecimal monthlyLimit;
        private BigDecimal actualSpend;
        private BigDecimal percentage;
        private String status;
    }
}
