package com.invoiceai.dto.request;

import com.invoiceai.model.enums.ExpenseStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ExportToEmailRequest {
    private ExpenseStatus status;
    private UUID categoryId;
    private String vendorName;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String search;
}
