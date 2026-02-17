package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {
    private UUID id;
    private String fileName;
    private long fileSize;
    private String fileType;
    private String status;
    private UserSummary uploadedBy;
    private ExpenseSummary expense;
    private String errorMessage;
    private Instant createdAt;

    @Getter
    @Builder
    public static class UserSummary {
        private UUID id;
        private String fullName;
    }

    @Getter
    @Builder
    public static class ExpenseSummary {
        private UUID id;
        private String vendorName;
        private java.math.BigDecimal amount;
    }
}
