package com.invoiceai.service;

import com.invoiceai.model.Expense;
import com.invoiceai.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final ExpenseRepository expenseRepository;

    /**
     * Checks if a new expense is a duplicate (same vendor + amount + date in same org).
     * Returns the first matching existing expense, or null if no duplicate found.
     * Flags only — does not block creation.
     */
    public Expense checkForDuplicate(UUID orgId, String vendorName, BigDecimal amount, LocalDate date) {
        List<Expense> duplicates = expenseRepository.findDuplicates(orgId, vendorName, amount, date);
        if (!duplicates.isEmpty()) {
            log.info("Duplicate detected for vendor={} amount={} date={} in org={}", vendorName, amount, date, orgId);
            return duplicates.getFirst();
        }
        return null;
    }

    /**
     * Same check but excludes a specific expense (for updates).
     */
    public Expense checkForDuplicateExcluding(UUID orgId, String vendorName, BigDecimal amount, LocalDate date, UUID excludeId) {
        List<Expense> duplicates = expenseRepository.findDuplicatesExcluding(orgId, vendorName, amount, date, excludeId);
        if (!duplicates.isEmpty()) {
            return duplicates.getFirst();
        }
        return null;
    }
}
