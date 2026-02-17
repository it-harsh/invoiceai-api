package com.invoiceai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class UpdateExpenseRequest {

    @Size(max = 255)
    private String vendorName;

    @DecimalMin("0.01")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency;

    @DecimalMin("0.00")
    private BigDecimal taxAmount;

    private LocalDate date;

    @Size(max = 1000)
    private String description;

    private UUID categoryId;
}
