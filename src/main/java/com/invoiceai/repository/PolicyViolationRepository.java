package com.invoiceai.repository;

import com.invoiceai.model.PolicyViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, UUID> {
    List<PolicyViolation> findByExpenseId(UUID expenseId);
    Page<PolicyViolation> findByOrganizationId(UUID organizationId, Pageable pageable);
}
