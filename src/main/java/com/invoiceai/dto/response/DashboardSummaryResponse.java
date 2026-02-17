package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardSummaryResponse {
    private BigDecimal totalSpend;
    private long expenseCount;
    private BigDecimal averageExpense;
    private TopCategory topCategory;
    private PeriodComparison vsLastPeriod;

    @Getter
    @Builder
    public static class TopCategory {
        private String name;
        private BigDecimal amount;
    }

    @Getter
    @Builder
    public static class PeriodComparison {
        private BigDecimal totalSpendChange;
        private BigDecimal expenseCountChange;
    }
}
