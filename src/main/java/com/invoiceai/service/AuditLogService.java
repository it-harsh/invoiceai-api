package com.invoiceai.service;

import com.invoiceai.dto.response.AuditLogResponse;
import com.invoiceai.model.AuditLog;
import com.invoiceai.model.Organization;
import com.invoiceai.model.User;
import com.invoiceai.repository.AuditLogRepository;
import com.invoiceai.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String entityType, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        UUID orgId = TenantContext.getCurrentOrgId();

        if (entityType != null && dateFrom != null && dateTo != null) {
            return auditLogRepository.findByOrganizationIdAndEntityTypeAndDateRange(
                    orgId, entityType, toInstant(dateFrom), toInstant(dateTo.plusDays(1)), pageable
            ).map(this::toResponse);
        }
        if (dateFrom != null && dateTo != null) {
            return auditLogRepository.findByOrganizationIdAndDateRange(
                    orgId, toInstant(dateFrom), toInstant(dateTo.plusDays(1)), pageable
            ).map(this::toResponse);
        }
        if (entityType != null) {
            return auditLogRepository.findByOrganizationIdAndEntityType(orgId, entityType, pageable)
                    .map(this::toResponse);
        }
        return auditLogRepository.findByOrganizationId(orgId, pageable).map(this::toResponse);
    }

    @Transactional
    public void log(User user, String entityType, UUID entityId, String action, String changes) {
        UUID orgId = TenantContext.getCurrentOrgId();
        AuditLog auditLog = AuditLog.builder()
                .organization(Organization.builder().id(orgId).build())
                .user(user)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changes(changes)
                .build();
        auditLogRepository.save(auditLog);
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse.AuditLogResponseBuilder builder = AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .changes(log.getChanges())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt());

        if (log.getUser() != null) {
            builder.user(AuditLogResponse.UserSummary.builder()
                    .id(log.getUser().getId())
                    .fullName(log.getUser().getFullName())
                    .build());
        }

        return builder.build();
    }
}
