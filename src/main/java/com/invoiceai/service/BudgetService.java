package com.invoiceai.service;

import com.invoiceai.dto.request.CreateBudgetRequest;
import com.invoiceai.dto.request.UpdateBudgetRequest;
import com.invoiceai.dto.response.BudgetProgressResponse;
import com.invoiceai.dto.response.BudgetResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.*;
import com.invoiceai.model.enums.BudgetAlertType;
import com.invoiceai.repository.BudgetAlertRepository;
import com.invoiceai.repository.BudgetRepository;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets() {
        UUID orgId = TenantContext.getCurrentOrgId();
        return budgetRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Budget budget = Budget.builder()
                .organization(Organization.builder().id(orgId).build())
                .category(category)
                .monthlyLimit(request.getMonthlyLimit())
                .alertAt80(request.getAlertAt80() != null ? request.getAlertAt80() : true)
                .alertAt100(request.getAlertAt100() != null ? request.getAlertAt100() : true)
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Budget budget = budgetRepository.findByIdAndOrganizationId(budgetId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (request.getMonthlyLimit() != null) budget.setMonthlyLimit(request.getMonthlyLimit());
        if (request.getAlertAt80() != null) budget.setAlertAt80(request.getAlertAt80());
        if (request.getAlertAt100() != null) budget.setAlertAt100(request.getAlertAt100());
        if (request.getIsActive() != null) budget.setActive(request.getIsActive());

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID budgetId) {
        UUID orgId = TenantContext.getCurrentOrgId();
        Budget budget = budgetRepository.findByIdAndOrganizationId(budgetId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public BudgetProgressResponse getBudgetProgress() {
        UUID orgId = TenantContext.getCurrentOrgId();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<Budget> activeBudgets = budgetRepository.findByOrganizationIdAndIsActiveTrue(orgId);
        BudgetProgressResponse.BudgetItem overall = null;
        List<BudgetProgressResponse.BudgetItem> byCategory = new ArrayList<>();

        for (Budget budget : activeBudgets) {
            BigDecimal actualSpend;
            String categoryName;
            String categoryColor;

            if (budget.getCategory() == null) {
                // Overall budget
                actualSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, monthStart, monthEnd);
                categoryName = "Overall";
                categoryColor = null;
            } else {
                actualSpend = expenseRepository.sumApprovedAmountByCategoryAndDateRange(
                        orgId, budget.getCategory().getId(), monthStart, monthEnd);
                categoryName = budget.getCategory().getName();
                categoryColor = budget.getCategory().getColor();
            }

            BigDecimal percentage = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                    ? actualSpend.multiply(BigDecimal.valueOf(100)).divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String status = percentage.compareTo(BigDecimal.valueOf(100)) >= 0 ? "EXCEEDED"
                    : percentage.compareTo(BigDecimal.valueOf(80)) >= 0 ? "WARNING" : "OK";

            BudgetProgressResponse.BudgetItem item = BudgetProgressResponse.BudgetItem.builder()
                    .budgetId(budget.getId())
                    .categoryName(categoryName)
                    .categoryColor(categoryColor)
                    .monthlyLimit(budget.getMonthlyLimit())
                    .actualSpend(actualSpend)
                    .percentage(percentage)
                    .status(status)
                    .build();

            if (budget.getCategory() == null) {
                overall = item;
            } else {
                byCategory.add(item);
            }
        }

        return BudgetProgressResponse.builder()
                .overall(overall)
                .byCategory(byCategory)
                .build();
    }

    /**
     * Check budgets after expense is approved. Fires alerts once per threshold per month.
     */
    @Transactional
    public void checkBudgetsAfterApproval(Expense expense) {
        UUID orgId = expense.getOrganization().getId();
        LocalDate monthStart = expense.getDate().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<Budget> budgetsToCheck = new ArrayList<>();

        // Overall budget
        budgetRepository.findByOrganizationIdAndCategoryIdIsNullAndIsActiveTrue(orgId)
                .ifPresent(budgetsToCheck::add);

        // Category-specific budget
        if (expense.getCategory() != null) {
            budgetRepository.findByOrganizationIdAndCategoryIdAndIsActiveTrue(orgId, expense.getCategory().getId())
                    .ifPresent(budgetsToCheck::add);
        }

        for (Budget budget : budgetsToCheck) {
            BigDecimal actualSpend;
            if (budget.getCategory() == null) {
                actualSpend = expenseRepository.sumApprovedAmountByDateRange(orgId, monthStart, monthEnd);
            } else {
                actualSpend = expenseRepository.sumApprovedAmountByCategoryAndDateRange(
                        orgId, budget.getCategory().getId(), monthStart, monthEnd);
            }

            BigDecimal percentage = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                    ? actualSpend.multiply(BigDecimal.valueOf(100)).divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Check 100% threshold
            if (budget.isAlertAt100() && percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                fireAlertOnce(budget, BudgetAlertType.THRESHOLD_100, monthStart, actualSpend, percentage);
            }
            // Check 80% threshold
            else if (budget.isAlertAt80() && percentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
                fireAlertOnce(budget, BudgetAlertType.THRESHOLD_80, monthStart, actualSpend, percentage);
            }
        }
    }

    private void fireAlertOnce(Budget budget, BudgetAlertType alertType, LocalDate month,
                               BigDecimal actualAmount, BigDecimal percentage) {
        // Unique constraint prevents duplicate alerts
        if (budgetAlertRepository.findByBudgetIdAndAlertTypeAndMonth(budget.getId(), alertType, month).isPresent()) {
            return;
        }

        BudgetAlert alert = BudgetAlert.builder()
                .organization(budget.getOrganization())
                .budget(budget)
                .alertType(alertType)
                .month(month)
                .actualAmount(actualAmount)
                .budgetAmount(budget.getMonthlyLimit())
                .percentage(percentage)
                .build();
        budgetAlertRepository.save(alert);

        emailNotificationService.sendBudgetAlertNotification(budget, alert);
        log.info("Budget alert fired: {} for budget {} month {}", alertType, budget.getId(), month);
    }

    private BudgetResponse toResponse(Budget budget) {
        BudgetResponse.BudgetResponseBuilder builder = BudgetResponse.builder()
                .id(budget.getId())
                .monthlyLimit(budget.getMonthlyLimit())
                .alertAt80(budget.isAlertAt80())
                .alertAt100(budget.isAlertAt100())
                .isActive(budget.isActive())
                .createdAt(budget.getCreatedAt());

        if (budget.getCategory() != null) {
            builder.category(BudgetResponse.CategorySummary.builder()
                    .id(budget.getCategory().getId())
                    .name(budget.getCategory().getName())
                    .color(budget.getCategory().getColor())
                    .build());
        }

        return builder.build();
    }
}
