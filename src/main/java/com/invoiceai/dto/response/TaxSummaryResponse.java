package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class TaxSummaryResponse {
    private BigDecimal totalTax;
    private List<TaxByCategory> byCategory;
    private List<TaxByVendor> byVendor;

    @Getter
    @Builder
    public static class TaxByCategory {
        private String categoryName;
        private BigDecimal taxAmount;
    }

    @Getter
    @Builder
    public static class TaxByVendor {
        private String vendorName;
        private BigDecimal taxAmount;
    }
}
