CREATE TABLE organizations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    slug                    VARCHAR(100) NOT NULL UNIQUE,
    owner_id                UUID NOT NULL REFERENCES users(id),
    plan                    VARCHAR(50) NOT NULL DEFAULT 'FREE',
    stripe_customer_id      VARCHAR(255),
    stripe_subscription_id  VARCHAR(255),
    monthly_invoice_count   INT NOT NULL DEFAULT 0,
    storage_used_bytes      BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_owner ON organizations(owner_id);
