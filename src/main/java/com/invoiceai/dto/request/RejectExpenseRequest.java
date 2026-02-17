package com.invoiceai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectExpenseRequest {

    @NotBlank
    private String reason;
}
