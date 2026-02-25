package com.invoiceai.repository;

import com.invoiceai.model.BudgetAlert;
import com.invoiceai.model.enums.BudgetAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, UUID> {
    Optional<BudgetAlert> findByBudgetIdAndAlertTypeAndMonth(UUID budgetId, BudgetAlertType alertType, LocalDate month);
    List<BudgetAlert> findByOrganizationIdAndMonthOrderByCreatedAtDesc(UUID organizationId, LocalDate month);
}
