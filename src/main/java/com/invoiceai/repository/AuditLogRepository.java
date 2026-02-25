package com.invoiceai.repository;

import com.invoiceai.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndEntityTypeAndEntityId(UUID organizationId, String entityType, UUID entityId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndEntityType(UUID organizationId, String entityType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND a.createdAt >= :from AND a.createdAt < :to ORDER BY a.createdAt DESC")
    Page<AuditLog> findByOrganizationIdAndDateRange(UUID orgId, Instant from, Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND a.entityType = :entityType AND a.createdAt >= :from AND a.createdAt < :to ORDER BY a.createdAt DESC")
    Page<AuditLog> findByOrganizationIdAndEntityTypeAndDateRange(UUID orgId, String entityType, Instant from, Instant to, Pageable pageable);
}
