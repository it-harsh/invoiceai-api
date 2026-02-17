package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SpendByCategoryResponse {
    private List<CategorySpend> categories;

    @Getter
    @Builder
    public static class CategorySpend {
        private String name;
        private String color;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
