package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class TopVendorsResponse {
    private List<VendorSpend> vendors;

    @Getter
    @Builder
    public static class VendorSpend {
        private String name;
        private BigDecimal amount;
        private long count;
    }
}
