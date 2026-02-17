package com.invoiceai.service;

import com.invoiceai.dto.response.DashboardSummaryResponse;
import com.invoiceai.dto.response.MonthlyTrendResponse;
import com.invoiceai.dto.response.SpendByCategoryResponse;
import com.invoiceai.dto.response.TopVendorsResponse;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to) {
        UUID orgId = TenantContext.getCurrentOrgId();

        BigDecimal totalSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, from, to);
        long expenseCount = expenseRepository.countApprovedByDateRange(orgId, from, to);

        BigDecimal averageExpense = expenseCount > 0
                ? totalSpend.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Top category
        List<Object[]> byCategory = expenseRepository.sumByCategoryAndDateRange(orgId, from, to);
        DashboardSummaryResponse.TopCategory topCategory = null;
        if (!byCategory.isEmpty()) {
            Object[] top = byCategory.get(0);
            topCategory = DashboardSummaryResponse.TopCategory.builder()
                    .name((String) top[0])
                    .amount((BigDecimal) top[2])
                    .build();
        }

        // Previous period comparison
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        LocalDate prevFrom = from.minusDays(periodDays);
        LocalDate prevTo = from.minusDays(1);

        BigDecimal prevSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, prevFrom, prevTo);
        long prevCount = expenseRepository.countApprovedByDateRange(orgId, prevFrom, prevTo);

        BigDecimal spendChange = BigDecimal.ZERO;
        if (prevSpend.compareTo(BigDecimal.ZERO) > 0) {
            spendChange = totalSpend.subtract(prevSpend)
                    .divide(prevSpend, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal countChange = BigDecimal.ZERO;
        if (prevCount > 0) {
            countChange = BigDecimal.valueOf(expenseCount - prevCount)
                    .divide(BigDecimal.valueOf(prevCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        return DashboardSummaryResponse.builder()
                .totalSpend(totalSpend)
                .expenseCount(expenseCount)
                .averageExpense(averageExpense)
                .topCategory(topCategory)
                .vsLastPeriod(DashboardSummaryResponse.PeriodComparison.builder()
                        .totalSpendChange(spendChange)
                        .expenseCountChange(countChange)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public SpendByCategoryResponse getSpendByCategory(LocalDate from, LocalDate to) {
        UUID orgId = TenantContext.getCurrentOrgId();

        List<Object[]> results = expenseRepository.sumByCategoryAndDateRange(orgId, from, to);

        BigDecimal total = results.stream()
                .map(r -> (BigDecimal) r[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SpendByCategoryResponse.CategorySpend> categories = results.stream()
                .map(r -> {
                    BigDecimal amount = (BigDecimal) r[2];
                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return SpendByCategoryResponse.CategorySpend.builder()
                            .name((String) r[0])
                            .color((String) r[1])
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .toList();

        return SpendByCategoryResponse.builder().categories(categories).build();
    }

    @Transactional(readOnly = true)
    public MonthlyTrendResponse getMonthlyTrend() {
        UUID orgId = TenantContext.getCurrentOrgId();
        LocalDate from = LocalDate.now().minusMonths(12).withDayOfMonth(1);

        List<Object[]> results = expenseRepository.monthlyTrend(orgId, from);

        List<MonthlyTrendResponse.MonthData> months = results.stream()
                .map(r -> MonthlyTrendResponse.MonthData.builder()
                        .month((String) r[0])
                        .amount((BigDecimal) r[1])
                        .count((Long) r[2])
                        .build())
                .toList();

        return MonthlyTrendResponse.builder().months(months).build();
    }

    @Transactional(readOnly = true)
    public TopVendorsResponse getTopVendors(int limit, LocalDate from, LocalDate to) {
        UUID orgId = TenantContext.getCurrentOrgId();

        List<Object[]> results = expenseRepository.topVendors(orgId, from, to, PageRequest.of(0, limit));

        List<TopVendorsResponse.VendorSpend> vendors = results.stream()
                .map(r -> TopVendorsResponse.VendorSpend.builder()
                        .name((String) r[0])
                        .amount((BigDecimal) r[1])
                        .count((Long) r[2])
                        .build())
                .toList();

        return TopVendorsResponse.builder().vendors(vendors).build();
    }
}
