package com.invoiceai.repository;

import com.invoiceai.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<Budget> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Budget> findByOrganizationIdAndCategoryIdIsNullAndIsActiveTrue(UUID organizationId);
    Optional<Budget> findByOrganizationIdAndCategoryIdAndIsActiveTrue(UUID organizationId, UUID categoryId);
    List<Budget> findByOrganizationId(UUID organizationId);
}
