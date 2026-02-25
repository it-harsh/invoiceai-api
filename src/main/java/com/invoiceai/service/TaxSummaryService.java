package com.invoiceai.service;

import com.invoiceai.dto.response.TaxSummaryResponse;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxSummaryService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public TaxSummaryResponse getTaxSummary(LocalDate dateFrom, LocalDate dateTo) {
        UUID orgId = TenantContext.getCurrentOrgId();

        BigDecimal totalTax = expenseRepository.sumTaxByDateRange(orgId, dateFrom, dateTo);

        List<TaxSummaryResponse.TaxByCategory> byCategory = expenseRepository
                .sumTaxByCategory(orgId, dateFrom, dateTo)
                .stream()
                .map(row -> TaxSummaryResponse.TaxByCategory.builder()
                        .categoryName(row[0] != null ? (String) row[0] : "Uncategorized")
                        .taxAmount((BigDecimal) row[1])
                        .build())
                .toList();

        List<TaxSummaryResponse.TaxByVendor> byVendor = expenseRepository
                .sumTaxByVendor(orgId, dateFrom, dateTo)
                .stream()
                .map(row -> TaxSummaryResponse.TaxByVendor.builder()
                        .vendorName((String) row[0])
                        .taxAmount((BigDecimal) row[1])
                        .build())
                .toList();

        return TaxSummaryResponse.builder()
                .totalTax(totalTax)
                .byCategory(byCategory)
                .byVendor(byVendor)
                .build();
    }
}
