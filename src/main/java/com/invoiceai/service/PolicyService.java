package com.invoiceai.service;

import com.invoiceai.dto.request.CreatePolicyRequest;
import com.invoiceai.dto.request.UpdatePolicyRequest;
import com.invoiceai.dto.response.PolicyResponse;
import com.invoiceai.dto.response.PolicyViolationResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.model.*;
import com.invoiceai.model.enums.PolicyRuleType;
import com.invoiceai.repository.CategoryRepository;
import com.invoiceai.repository.ExpensePolicyRepository;
import com.invoiceai.repository.ExpenseRepository;
import com.invoiceai.repository.PolicyViolationRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final ExpensePolicyRepository policyRepository;
    private final PolicyViolationRepository violationRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional(readOnly = true)
    public List<PolicyResponse> getPolicies() {
        UUID orgId = TenantContext.getCurrentOrgId();
        return policyRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        ExpensePolicy policy = ExpensePolicy.builder()
                .organization(Organization.builder().id(orgId).build())
                .name(request.getName())
                .ruleType(request.getRuleType())
                .category(category)
                .thresholdAmount(request.getThresholdAmount())
                .requiredField(request.getRequiredField())
                .build();

        return toResponse(policyRepository.save(policy));
    }

    @Transactional
    public PolicyResponse updatePolicy(UUID policyId, UpdatePolicyRequest request) {
        UUID orgId = TenantContext.getCurrentOrgId();
        ExpensePolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if (request.getName() != null) policy.setName(request.getName());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> c.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            policy.setCategory(category);
        }
        if (request.getThresholdAmount() != null) policy.setThresholdAmount(request.getThresholdAmount());
        if (request.getIsActive() != null) policy.setActive(request.getIsActive());

        return toResponse(policyRepository.save(policy));
    }

    @Transactional
    public void deletePolicy(UUID policyId) {
        UUID orgId = TenantContext.getCurrentOrgId();
        ExpensePolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        policyRepository.delete(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyViolationResponse> getViolations(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentOrgId();
        return violationRepository.findByOrganizationId(orgId, pageable)
                .map(this::toViolationResponse);
    }

    /**
     * Evaluate an expense against all active policies.
     * Called during expense creation — violations saved synchronously, email sent async.
     */
    @Transactional
    public List<PolicyViolation> evaluateExpense(Expense expense) {
        UUID orgId = expense.getOrganization().getId();
        List<ExpensePolicy> activePolicies = policyRepository.findByOrganizationIdAndIsActiveTrue(orgId);
        List<PolicyViolation> violations = new ArrayList<>();

        for (ExpensePolicy policy : activePolicies) {
            String violation = checkPolicy(policy, expense, orgId);
            if (violation != null) {
                PolicyViolation pv = PolicyViolation.builder()
                        .organization(expense.getOrganization())
                        .expense(expense)
                        .policy(policy)
                        .violationMessage(violation)
                        .build();
                violations.add(violationRepository.save(pv));
            }
        }

        if (!violations.isEmpty()) {
            emailNotificationService.sendPolicyViolationNotification(expense, violations);
        }

        return violations;
    }

    private String checkPolicy(ExpensePolicy policy, Expense expense, UUID orgId) {
        return switch (policy.getRuleType()) {
            case MAX_AMOUNT_PER_EXPENSE -> {
                if (policy.getThresholdAmount() != null && expense.getAmount().compareTo(policy.getThresholdAmount()) > 0) {
                    if (policy.getCategory() == null || (expense.getCategory() != null &&
                            expense.getCategory().getId().equals(policy.getCategory().getId()))) {
                        yield String.format("Expense amount $%s exceeds limit of $%s (%s)",
                                expense.getAmount(), policy.getThresholdAmount(), policy.getName());
                    }
                }
                yield null;
            }
            case MAX_AMOUNT_PER_CATEGORY_MONTHLY -> {
                if (policy.getThresholdAmount() != null && expense.getCategory() != null) {
                    if (policy.getCategory() == null || expense.getCategory().getId().equals(policy.getCategory().getId())) {
                        LocalDate monthStart = expense.getDate().withDayOfMonth(1);
                        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                        UUID catId = expense.getCategory().getId();
                        BigDecimal monthlyTotal = expenseRepository.sumApprovedAmountByCategoryAndDateRange(
                                orgId, catId, monthStart, monthEnd);
                        BigDecimal projected = monthlyTotal.add(expense.getAmount());
                        if (projected.compareTo(policy.getThresholdAmount()) > 0) {
                            yield String.format("Category monthly spend $%s (with this expense) exceeds limit of $%s (%s)",
                                    projected, policy.getThresholdAmount(), policy.getName());
                        }
                    }
                }
                yield null;
            }
            case REQUIRED_FIELD -> {
                if (policy.getRequiredField() != null) {
                    boolean missing = switch (policy.getRequiredField()) {
                        case "description" -> expense.getDescription() == null || expense.getDescription().isBlank();
                        case "category" -> expense.getCategory() == null;
                        default -> false;
                    };
                    if (missing) {
                        yield String.format("Required field '%s' is missing (%s)",
                                policy.getRequiredField(), policy.getName());
                    }
                }
                yield null;
            }
        };
    }

    private PolicyResponse toResponse(ExpensePolicy policy) {
        PolicyResponse.PolicyResponseBuilder builder = PolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .ruleType(policy.getRuleType().name())
                .thresholdAmount(policy.getThresholdAmount())
                .requiredField(policy.getRequiredField())
                .isActive(policy.isActive())
                .createdAt(policy.getCreatedAt());

        if (policy.getCategory() != null) {
            builder.category(PolicyResponse.CategorySummary.builder()
                    .id(policy.getCategory().getId())
                    .name(policy.getCategory().getName())
                    .build());
        }

        return builder.build();
    }

    private PolicyViolationResponse toViolationResponse(PolicyViolation violation) {
        return PolicyViolationResponse.builder()
                .id(violation.getId())
                .expenseId(violation.getExpense().getId())
                .vendorName(violation.getExpense().getVendorName())
                .expenseAmount(violation.getExpense().getAmount())
                .policyName(violation.getPolicy().getName())
                .violationMessage(violation.getViolationMessage())
                .createdAt(violation.getCreatedAt())
                .build();
    }
}
