package com.invoiceai.service;

import com.invoiceai.dto.request.CreateRecurringExpenseRequest;
import com.invoiceai.dto.response.RecurringExpenseResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.*;
import com.invoiceai.model.enums.ExpenseStatus;
import com.invoiceai.model.enums.RecurringFrequency;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.repository.RecurringExpenseRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getRecurringExpenses() {
        UUID orgId = TenantContext.getCurrentOrgId();
        return recurringExpenseRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(CreateRecurringExpenseRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Expense sourceExpense = null;
        if (request.getSourceExpenseId() != null) {
            sourceExpense = expenseRepository.findByIdAndOrganizationId(request.getSourceExpenseId(), orgId)
                    .orElse(null);
        }

        RecurringExpense recurring = RecurringExpense.builder()
                .organization(Organization.builder().id(orgId).build())
                .sourceExpense(sourceExpense)
                .vendorName(request.getVendorName().trim())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .description(request.getDescription())
                .category(category)
                .frequency(request.getFrequency())
                .nextDueDate(request.getStartDate())
                .build();

        return toResponse(recurringExpenseRepository.save(recurring));
    }

    @Transactional
    public void deleteRecurringExpense(UUID recurringId) {
        UUID orgId = TenantContext.getCurrentOrgId();
        RecurringExpense recurring = recurringExpenseRepository.findByIdAndOrganizationId(recurringId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found"));
        recurringExpenseRepository.delete(recurring);
    }

    /**
     * Process all due recurring expenses — called by scheduler (no TenantContext).
     * Creates expenses with APPROVED status.
     */
    @Transactional
    public int processDueRecurringExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpense> dueItems = recurringExpenseRepository.findByIsActiveTrueAndNextDueDateLessThanEqual(today);
        int processed = 0;

        for (RecurringExpense recurring : dueItems) {
            try {
                Expense expense = Expense.builder()
                        .organization(recurring.getOrganization())
                        .vendorName(recurring.getVendorName())
                        .amount(recurring.getAmount())
                        .currency(recurring.getCurrency())
                        .taxAmount(recurring.getTaxAmount())
                        .date(recurring.getNextDueDate())
                        .description(recurring.getDescription())
                        .category(recurring.getCategory())
                        .status(ExpenseStatus.APPROVED)
                        .build();
                expenseRepository.save(expense);

                recurring.setLastCreatedAt(Instant.now());
                recurring.setNextDueDate(calculateNextDueDate(recurring.getNextDueDate(), recurring.getFrequency()));
                recurringExpenseRepository.save(recurring);

                processed++;
                log.info("Created recurring expense for vendor={} amount={} org={}",
                        recurring.getVendorName(), recurring.getAmount(), recurring.getOrganization().getId());
            } catch (Exception e) {
                log.error("Failed to process recurring expense {}", recurring.getId(), e);
            }
        }

        return processed;
    }

    private LocalDate calculateNextDueDate(LocalDate current, RecurringFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }

    private RecurringExpenseResponse toResponse(RecurringExpense recurring) {
        RecurringExpenseResponse.RecurringExpenseResponseBuilder builder = RecurringExpenseResponse.builder()
                .id(recurring.getId())
                .vendorName(recurring.getVendorName())
                .amount(recurring.getAmount())
                .currency(recurring.getCurrency())
                .taxAmount(recurring.getTaxAmount())
                .description(recurring.getDescription())
                .frequency(recurring.getFrequency().name())
                .nextDueDate(recurring.getNextDueDate())
                .lastCreatedAt(recurring.getLastCreatedAt())
                .isActive(recurring.isActive())
                .createdAt(recurring.getCreatedAt());

        if (recurring.getCategory() != null) {
            builder.category(RecurringExpenseResponse.CategorySummary.builder()
                    .id(recurring.getCategory().getId())
                    .name(recurring.getCategory().getName())
                    .color(recurring.getCategory().getColor())
                    .build());
        }

        return builder.build();
    }
}
