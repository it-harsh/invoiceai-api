package com.invoiceai.controller;

import com.invoiceai.dto.request.CreateRecurringExpenseRequest;
import com.invoiceai.dto.response.RecurringExpenseResponse;
import com.invoiceai.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpenses() {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpenses());
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            @Valid @RequestBody CreateRecurringExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringExpenseService.createRecurringExpense(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable UUID id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }
}
