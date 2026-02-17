package com.invoiceai.service;

import com.invoiceai.dto.request.CreateExpenseRequest;
import com.invoiceai.dto.request.UpdateExpenseRequest;
import com.invoiceai.dto.response.ExpenseResponse;
import com.invoiceai.exception.BadRequestException;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.*;
import com.invoiceai.model.enums.ExpenseStatus;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.repository.ExpenseSpecification;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(
            ExpenseStatus status, UUID categoryId, String vendorName,
            LocalDate dateFrom, LocalDate dateTo,
            BigDecimal amountMin, BigDecimal amountMax,
            String search, Pageable pageable) {

        UUID orgId = TenantContext.getCurrentOrgId();

        return expenseRepository.findAll(
                ExpenseSpecification.withFilters(orgId, status, categoryId, vendorName,
                        dateFrom, dateTo, amountMin, amountMax, search),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID expenseId) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Expense expense = expenseRepository.findByIdAndOrganizationId(expenseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Expense expense = Expense.builder()
                .organization(Organization.builder().id(orgId).build())
                .vendorName(request.getVendorName().trim())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .date(request.getDate())
                .description(request.getDescription())
                .category(category)
                .status(ExpenseStatus.NEEDS_REVIEW)
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Expense expense = expenseRepository.findByIdAndOrganizationId(expenseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (request.getVendorName() != null) expense.setVendorName(request.getVendorName().trim());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency());
        if (request.getTaxAmount() != null) expense.setTaxAmount(request.getTaxAmount());
        if (request.getDate() != null) expense.setDate(request.getDate());
        if (request.getDescription() != null) expense.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            expense.setCategory(category);
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse approveExpense(UUID expenseId, User reviewer) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Expense expense = expenseRepository.findByIdAndOrganizationId(expenseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (expense.getStatus() != ExpenseStatus.NEEDS_REVIEW) {
            throw new BadRequestException("Only expenses with NEEDS_REVIEW status can be approved");
        }

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setReviewedBy(reviewer);
        expense.setReviewedAt(Instant.now());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse rejectExpense(UUID expenseId, String reason, User reviewer) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Expense expense = expenseRepository.findByIdAndOrganizationId(expenseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (expense.getStatus() != ExpenseStatus.NEEDS_REVIEW) {
            throw new BadRequestException("Only expenses with NEEDS_REVIEW status can be rejected");
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setReviewedBy(reviewer);
        expense.setReviewedAt(Instant.now());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(UUID expenseId) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Expense expense = expenseRepository.findByIdAndOrganizationId(expenseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesForExport(
            ExpenseStatus status, UUID categoryId, String vendorName,
            LocalDate dateFrom, LocalDate dateTo,
            BigDecimal amountMin, BigDecimal amountMax, String search) {

        UUID orgId = TenantContext.getCurrentOrgId();
        return expenseRepository.findAll(
                ExpenseSpecification.withFilters(orgId, status, categoryId, vendorName,
                        dateFrom, dateTo, amountMin, amountMax, search));
    }

    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse.ExpenseResponseBuilder builder = ExpenseResponse.builder()
                .id(expense.getId())
                .vendorName(expense.getVendorName())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .taxAmount(expense.getTaxAmount())
                .date(expense.getDate())
                .description(expense.getDescription())
                .status(expense.getStatus().name())
                .aiConfidence(expense.getAiConfidence())
                .createdAt(expense.getCreatedAt());

        if (expense.getCategory() != null) {
            builder.category(ExpenseResponse.CategorySummary.builder()
                    .id(expense.getCategory().getId())
                    .name(expense.getCategory().getName())
                    .color(expense.getCategory().getColor())
                    .build());
        }

        if (expense.getInvoice() != null) {
            builder.invoice(ExpenseResponse.InvoiceSummary.builder()
                    .id(expense.getInvoice().getId())
                    .fileName(expense.getInvoice().getFileName())
                    .build());
        }

        if (expense.getLineItems() != null && !expense.getLineItems().isEmpty()) {
            builder.lineItems(expense.getLineItems().stream()
                    .map(li -> ExpenseResponse.LineItemResponse.builder()
                            .id(li.getId())
                            .description(li.getDescription())
                            .quantity(li.getQuantity())
                            .unitPrice(li.getUnitPrice())
                            .total(li.getTotal())
                            .build())
                    .toList());
        }

        return builder.build();
    }
}
