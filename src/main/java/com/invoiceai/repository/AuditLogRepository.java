package com.invoiceai.repository;

import com.invoiceai.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndEntityTypeAndEntityId(UUID organizationId, String entityType, UUID entityId, Pageable pageable);
}
