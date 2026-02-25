package com.invoiceai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdatePolicyRequest {
    @Size(max = 255)
    private String name;

    private UUID categoryId;

    @DecimalMin("0.01")
    private BigDecimal thresholdAmount;

    private Boolean isActive;
}
