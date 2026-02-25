package com.invoiceai.service;

import com.invoiceai.dto.request.BulkCreateExpenseRequest;
import com.invoiceai.dto.request.CreateExpenseRequest;
import com.invoiceai.dto.request.UpdateExpenseRequest;
import com.invoiceai.dto.response.BulkCreateExpenseResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final VendorService vendorService;
    private final PolicyService policyService;
    private final BudgetService budgetService;
    private final EmailNotificationService emailNotificationService;

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

        // Duplicate detection — flag only, don't block
        Expense duplicate = duplicateDetectionService.checkForDuplicate(
                orgId, expense.getVendorName(), expense.getAmount(), expense.getDate());
        if (duplicate != null) {
            expense.setDuplicate(true);
            expense.setDuplicateOf(duplicate);
        }

        Expense saved = expenseRepository.save(expense);

        // Auto-maintain vendor directory
        vendorService.upsertFromExpense(orgId, saved.getVendorName(), saved.getAmount(), saved.getDate(), category);

        // Evaluate expense against active policies
        policyService.evaluateExpense(saved);

        return toResponse(saved);
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

        Expense saved = expenseRepository.save(expense);

        // Check budget thresholds after approval
        budgetService.checkBudgetsAfterApproval(saved);

        return toResponse(saved);
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

    @Transactional
    public BulkCreateExpenseResponse bulkCreateExpenses(BulkCreateExpenseRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        List<ExpenseResponse> created = new ArrayList<>();
        List<BulkCreateExpenseResponse.BulkError> errors = new ArrayList<>();
        int duplicates = 0;

        for (int i = 0; i < request.getExpenses().size(); i++) {
            CreateExpenseRequest item = request.getExpenses().get(i);
            try {
                Category category = null;
                if (item.getCategoryId() != null) {
                    category = categoryRepository.findById(item.getCategoryId())
                            .filter(c -> c.getOrganization().getId().equals(orgId))
                            .orElse(null);
                }

                Expense expense = Expense.builder()
                        .organization(Organization.builder().id(orgId).build())
                        .vendorName(item.getVendorName().trim())
                        .amount(item.getAmount())
                        .currency(item.getCurrency())
                        .taxAmount(item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO)
                        .date(item.getDate())
                        .description(item.getDescription())
                        .category(category)
                        .status(ExpenseStatus.NEEDS_REVIEW)
                        .build();

                Expense duplicate = duplicateDetectionService.checkForDuplicate(
                        orgId, expense.getVendorName(), expense.getAmount(), expense.getDate());
                if (duplicate != null) {
                    expense.setDuplicate(true);
                    expense.setDuplicateOf(duplicate);
                    duplicates++;
                }

                Expense saved = expenseRepository.save(expense);
                vendorService.upsertFromExpense(orgId, saved.getVendorName(), saved.getAmount(), saved.getDate(), category);
                created.add(toResponse(saved));
            } catch (Exception e) {
                errors.add(BulkCreateExpenseResponse.BulkError.builder()
                        .index(i)
                        .message(e.getMessage())
                        .build());
            }
        }

        return BulkCreateExpenseResponse.builder()
                .total(request.getExpenses().size())
                .created(created.size())
                .duplicates(duplicates)
                .expenses(created)
                .errors(errors)
                .build();
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

    public void exportToEmail(User user, ExpenseStatus status, UUID categoryId, String vendorName,
                              LocalDate dateFrom, LocalDate dateTo,
                              BigDecimal amountMin, BigDecimal amountMax, String search) {
        List<Expense> expenses = getExpensesForExport(status, categoryId, vendorName, dateFrom, dateTo, amountMin, amountMax, search);

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Vendor,Amount,Currency,Tax,Category,Status,Description\n");
        for (Expense e : expenses) {
            csv.append(String.format("%s,\"%s\",%s,%s,%s,\"%s\",%s,\"%s\"\n",
                    e.getDate(),
                    escapeCsv(e.getVendorName()),
                    e.getAmount(),
                    e.getCurrency(),
                    e.getTaxAmount(),
                    e.getCategory() != null ? escapeCsv(e.getCategory().getName()) : "",
                    e.getStatus(),
                    escapeCsv(e.getDescription() != null ? e.getDescription() : "")));
        }

        // Build HTML summary
        BigDecimal totalAmount = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String html = buildExportHtml(expenses.size(), totalAmount, dateFrom, dateTo);

        String subject = String.format("InvoiceAI — Expense Export (%d expenses)", expenses.size());
        emailNotificationService.sendExpenseExportEmail(user, subject, html, csv.toString().getBytes());
    }

    private String buildExportHtml(int count, BigDecimal total, LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        sb.append("<h2>Expense Export</h2>");
        sb.append("<table style='border-collapse: collapse; width: 100%; max-width: 600px;'>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Total Expenses:</td><td style='padding: 8px;'>").append(count).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Total Amount:</td><td style='padding: 8px;'>$").append(total).append("</td></tr>");
        if (dateFrom != null && dateTo != null) {
            sb.append("<tr><td style='padding: 8px; font-weight: bold;'>Period:</td><td style='padding: 8px;'>").append(dateFrom).append(" to ").append(dateTo).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p style='margin-top: 16px;'>The detailed CSV is attached to this email.</p>");
        sb.append("<hr style='border: none; border-top: 1px solid #E5E7EB; margin: 20px 0;'>");
        sb.append("<p style='color: #9CA3AF; font-size: 12px;'>InvoiceAI - Automated Export</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeCsv(String value) {
        return value.replace("\"", "\"\"");
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
                .isDuplicate(expense.isDuplicate())
                .duplicateOfId(expense.getDuplicateOf() != null ? expense.getDuplicateOf().getId() : null)
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
