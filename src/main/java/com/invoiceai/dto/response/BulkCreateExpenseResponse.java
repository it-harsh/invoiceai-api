package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BulkCreateExpenseResponse {
    private int total;
    private int created;
    private int duplicates;
    private List<ExpenseResponse> expenses;
    private List<BulkError> errors;

    @Getter
    @Builder
    public static class BulkError {
        private int index;
        private String message;
    }
}
