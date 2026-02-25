package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PolicyViolationResponse {
    private UUID id;
    private UUID expenseId;
    private String vendorName;
    private BigDecimal expenseAmount;
    private String policyName;
    private String violationMessage;
    private Instant createdAt;
}
