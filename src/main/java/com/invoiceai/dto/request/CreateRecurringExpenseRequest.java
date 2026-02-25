package com.invoiceai.dto.request;

import com.invoiceai.model.enums.RecurringFrequency;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateRecurringExpenseRequest {
    private UUID sourceExpenseId;

    @NotBlank @Size(max = 255)
    private String vendorName;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency = "USD";

    @DecimalMin("0.00")
    private BigDecimal taxAmount;

    @Size(max = 1000)
    private String description;

    private UUID categoryId;

    @NotNull
    private RecurringFrequency frequency;

    @NotNull
    private LocalDate startDate;
}
