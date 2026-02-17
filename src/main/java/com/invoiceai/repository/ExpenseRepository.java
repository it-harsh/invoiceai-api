package com.invoiceai.repository;

import com.invoiceai.model.Expense;
import com.invoiceai.model.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {
    Optional<Expense> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Expense> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to")
    BigDecimal sumApprovedAmountByDateRange(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to")
    long countApprovedByDateRange(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT e.category.name, e.category.color, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to GROUP BY e.category.name, e.category.color ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategoryAndDateRange(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT FUNCTION('TO_CHAR', e.date, 'YYYY-MM'), COALESCE(SUM(e.amount), 0), COUNT(e) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date >= :from GROUP BY FUNCTION('TO_CHAR', e.date, 'YYYY-MM') ORDER BY FUNCTION('TO_CHAR', e.date, 'YYYY-MM')")
    List<Object[]> monthlyTrend(UUID orgId, LocalDate from);

    @Query("SELECT e.vendorName, COALESCE(SUM(e.amount), 0), COUNT(e) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to GROUP BY e.vendorName ORDER BY SUM(e.amount) DESC")
    List<Object[]> topVendors(UUID orgId, LocalDate from, LocalDate to, Pageable pageable);

    Optional<Expense> findByInvoiceId(UUID invoiceId);
}
