package com.invoiceai.controller;

import com.invoiceai.dto.response.DashboardSummaryResponse;
import com.invoiceai.dto.response.MonthlyTrendResponse;
import com.invoiceai.dto.response.SpendByCategoryResponse;
import com.invoiceai.dto.response.TopVendorsResponse;
import com.invoiceai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(defaultValue = "current_month") String period,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {

        LocalDate[] range = resolveDateRange(period, dateFrom, dateTo);
        return ResponseEntity.ok(dashboardService.getSummary(range[0], range[1]));
    }

    @GetMapping("/spend-by-category")
    public ResponseEntity<SpendByCategoryResponse> getSpendByCategory(
            @RequestParam(defaultValue = "current_month") String period,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {

        LocalDate[] range = resolveDateRange(period, dateFrom, dateTo);
        return ResponseEntity.ok(dashboardService.getSpendByCategory(range[0], range[1]));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<MonthlyTrendResponse> getMonthlyTrend() {
        return ResponseEntity.ok(dashboardService.getMonthlyTrend());
    }

    @GetMapping("/top-vendors")
    public ResponseEntity<TopVendorsResponse> getTopVendors(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "current_month") String period,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {

        LocalDate[] range = resolveDateRange(period, dateFrom, dateTo);
        return ResponseEntity.ok(dashboardService.getTopVendors(limit, range[0], range[1]));
    }

    private LocalDate[] resolveDateRange(String period, LocalDate dateFrom, LocalDate dateTo) {
        if ("custom".equals(period) && dateFrom != null && dateTo != null) {
            return new LocalDate[]{dateFrom, dateTo};
        }

        LocalDate now = LocalDate.now();
        return switch (period) {
            case "last_month" -> new LocalDate[]{
                    now.minusMonths(1).withDayOfMonth(1),
                    now.withDayOfMonth(1).minusDays(1)
            };
            case "last_3_months" -> new LocalDate[]{
                    now.minusMonths(3).withDayOfMonth(1),
                    now
            };
            case "last_12_months" -> new LocalDate[]{
                    now.minusMonths(12).withDayOfMonth(1),
                    now
            };
            default -> new LocalDate[]{  // current_month
                    now.withDayOfMonth(1),
                    now
            };
        };
    }
}
