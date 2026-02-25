package com.invoiceai.dto.request;

import com.invoiceai.model.enums.PolicyRuleType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreatePolicyRequest {
    @NotBlank @Size(max = 255)
    private String name;

    @NotNull
    private PolicyRuleType ruleType;

    private UUID categoryId;

    @DecimalMin("0.01")
    private BigDecimal thresholdAmount;

    @Size(max = 100)
    private String requiredField;
}
