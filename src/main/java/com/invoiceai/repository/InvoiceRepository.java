package com.invoiceai.repository;

import com.invoiceai.model.Invoice;
import com.invoiceai.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Invoice> findByOrganizationIdAndStatus(UUID organizationId, InvoiceStatus status, Pageable pageable);
    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);
    int countByOrganizationId(UUID organizationId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.organization.id = :orgId AND MONTH(i.createdAt) = MONTH(CURRENT_TIMESTAMP) AND YEAR(i.createdAt) = YEAR(CURRENT_TIMESTAMP)")
    int countMonthlyInvoices(UUID orgId);
}
