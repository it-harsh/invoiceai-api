package com.invoiceai.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> currentOrgId = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID getCurrentOrgId() {
        UUID orgId = currentOrgId.get();
        if (orgId == null) {
            throw new IllegalStateException("No organization context set. Ensure X-Organization-Id header is present.");
        }
        return orgId;
    }

    public static void setCurrentOrgId(UUID orgId) {
        currentOrgId.set(orgId);
    }

    public static void clear() {
        currentOrgId.remove();
    }
}
