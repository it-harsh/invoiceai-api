package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ExpenseResponse {
    private UUID id;
    private String vendorName;
    private BigDecimal amount;
    private String currency;
    private BigDecimal taxAmount;
    private LocalDate date;
    private String description;
    private CategorySummary category;
    private String status;
    private BigDecimal aiConfidence;
    private InvoiceSummary invoice;
    private List<LineItemResponse> lineItems;
    private boolean isDuplicate;
    private UUID duplicateOfId;
    private List<PolicyViolationSummary> policyViolations;
    private Instant createdAt;

    @Getter
    @Builder
    public static class PolicyViolationSummary {
        private UUID policyId;
        private String policyName;
        private String message;
    }

    @Getter
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
        private String color;
    }

    @Getter
    @Builder
    public static class InvoiceSummary {
        private UUID id;
        private String fileName;
    }

    @Getter
    @Builder
    public static class LineItemResponse {
        private UUID id;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
    }
}
