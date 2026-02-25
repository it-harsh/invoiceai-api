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

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.category WHERE e.organization.id = :orgId ORDER BY e.date DESC, e.createdAt DESC")
    List<Expense> findRecentByOrganizationId(UUID orgId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'NEEDS_REVIEW'")
    long countPendingReview(UUID orgId);

    // Duplicate detection
    @Query("SELECT e FROM Expense e WHERE e.organization.id = :orgId AND e.vendorName = :vendorName AND e.amount = :amount AND e.date = :date")
    List<Expense> findDuplicates(UUID orgId, String vendorName, BigDecimal amount, LocalDate date);

    @Query("SELECT e FROM Expense e WHERE e.organization.id = :orgId AND e.vendorName = :vendorName AND e.amount = :amount AND e.date = :date AND e.id != :excludeId")
    List<Expense> findDuplicatesExcluding(UUID orgId, String vendorName, BigDecimal amount, LocalDate date, UUID excludeId);

    // Tax summary
    @Query("SELECT e.category.name, COALESCE(SUM(e.taxAmount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to GROUP BY e.category.name ORDER BY SUM(e.taxAmount) DESC")
    List<Object[]> sumTaxByCategory(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT e.vendorName, COALESCE(SUM(e.taxAmount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to GROUP BY e.vendorName ORDER BY SUM(e.taxAmount) DESC")
    List<Object[]> sumTaxByVendor(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.taxAmount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to")
    BigDecimal sumTaxByDateRange(UUID orgId, LocalDate from, LocalDate to);

    // Budget checking
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.organization.id = :orgId AND e.category.id = :categoryId AND e.status = 'APPROVED' AND e.date BETWEEN :from AND :to")
    BigDecimal sumApprovedAmountByCategoryAndDateRange(UUID orgId, UUID categoryId, LocalDate from, LocalDate to);
}
