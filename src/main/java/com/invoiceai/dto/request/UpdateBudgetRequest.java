package com.invoiceai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBudgetRequest {
    @DecimalMin("1.00")
    private BigDecimal monthlyLimit;

    private Boolean alertAt80;
    private Boolean alertAt100;
    private Boolean isActive;
}
