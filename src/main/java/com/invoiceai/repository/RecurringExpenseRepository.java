package com.invoiceai.repository;

import com.invoiceai.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {
    List<RecurringExpense> findByOrganizationId(UUID organizationId);
    Optional<RecurringExpense> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<RecurringExpense> findByIsActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
