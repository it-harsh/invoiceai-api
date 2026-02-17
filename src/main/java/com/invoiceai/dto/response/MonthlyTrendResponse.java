package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MonthlyTrendResponse {
    private List<MonthData> months;

    @Getter
    @Builder
    public static class MonthData {
        private String month;
        private BigDecimal amount;
        private long count;
    }
}
