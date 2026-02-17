package com.invoiceai.controller;

import com.invoiceai.dto.request.CreateExpenseRequest;
import com.invoiceai.dto.request.RejectExpenseRequest;
import com.invoiceai.dto.request.UpdateExpenseRequest;
import com.invoiceai.dto.response.ExpenseResponse;
import com.invoiceai.model.Expense;
import com.invoiceai.model.enums.ExpenseStatus;
import com.invoiceai.security.UserPrincipal;
import com.invoiceai.service.ExpenseService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpenses(
                status, categoryId, vendorName, dateFrom, dateTo,
                amountMin, amountMax, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.getExpense(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ExpenseResponse> approveExpense(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.approveExpense(id, principal.getUser()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ExpenseResponse> rejectExpense(
            @PathVariable UUID id,
            @Valid @RequestBody RejectExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.rejectExpense(id, request.getReason(), principal.getUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public void exportExpenses(
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String search,
            HttpServletResponse response) throws Exception {

        List<Expense> expenses = expenseService.getExpensesForExport(
                status, categoryId, vendorName, dateFrom, dateTo, amountMin, amountMax, search);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=expenses.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Date,Vendor,Amount,Currency,Tax,Category,Status,Description");

        for (Expense e : expenses) {
            writer.printf("%s,\"%s\",%s,%s,%s,\"%s\",%s,\"%s\"%n",
                    e.getDate(),
                    escapeCsv(e.getVendorName()),
                    e.getAmount(),
                    e.getCurrency(),
                    e.getTaxAmount(),
                    e.getCategory() != null ? escapeCsv(e.getCategory().getName()) : "",
                    e.getStatus(),
                    escapeCsv(e.getDescription() != null ? e.getDescription() : ""));
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        return value.replace("\"", "\"\"");
    }
}
