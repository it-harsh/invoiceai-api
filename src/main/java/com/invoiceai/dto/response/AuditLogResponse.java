package com.invoiceai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {
    private UUID id;
    private UserSummary user;
    private String entityType;
    private UUID entityId;
    private String action;
    private String changes;
    private String ipAddress;
    private Instant createdAt;

    @Getter
    @Builder
    public static class UserSummary {
        private UUID id;
        private String fullName;
    }
}
