package com.invoiceai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkCreateExpenseRequest {
    @NotNull
    @Size(min = 1, max = 500)
    @Valid
    private List<CreateExpenseRequest> expenses;
}
