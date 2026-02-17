package com.invoiceai.service.ai;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class ExtractionResult {
    private String vendorName;
    private BigDecimal amount;
    private String currency;
    private BigDecimal taxAmount;
    private LocalDate date;
    private String description;
    private String categoryHint;
    private BigDecimal confidence;
    private List<LineItem> lineItems;
    private String rawResponse;

    @Getter
    @Setter
    @Builder
    public static class LineItem {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
    }
}
