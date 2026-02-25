package com.invoiceai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateBudgetRequest {
    private UUID categoryId;

    @NotNull @DecimalMin("1.00")
    private BigDecimal monthlyLimit;

    private Boolean alertAt80;
    private Boolean alertAt100;
}
