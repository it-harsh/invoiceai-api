package com.invoiceai.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateExpenseRequest {

    @NotBlank
    @Size(max = 255)
    private String vendorName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency = "USD";

    @DecimalMin("0.00")
    private BigDecimal taxAmount;

    @NotNull
    private LocalDate date;

    @Size(max = 1000)
    private String description;

    private UUID categoryId;
}
