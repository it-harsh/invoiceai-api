package com.invoiceai.repository;

import com.invoiceai.model.ExpensePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpensePolicyRepository extends JpaRepository<ExpensePolicy, UUID> {
    List<ExpensePolicy> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<ExpensePolicy> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<ExpensePolicy> findByOrganizationId(UUID organizationId);
}
