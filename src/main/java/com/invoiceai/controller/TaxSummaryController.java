package com.invoiceai.controller;

import com.invoiceai.dto.response.TaxSummaryResponse;
import com.invoiceai.service.TaxSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class TaxSummaryController {

    private final TaxSummaryService taxSummaryService;

    @GetMapping("/tax-summary")
    public ResponseEntity<TaxSummaryResponse> getTaxSummary(
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo) {
        return ResponseEntity.ok(taxSummaryService.getTaxSummary(dateFrom, dateTo));
    }
}
