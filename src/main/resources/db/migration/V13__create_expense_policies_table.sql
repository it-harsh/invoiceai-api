CREATE TABLE expense_policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    rule_type       VARCHAR(50) NOT NULL,
    category_id     UUID REFERENCES categories(id) ON DELETE CASCADE,
    threshold_amount DECIMAL(12, 2),
    required_field  VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_policies_org ON expense_policies(organization_id);
CREATE INDEX idx_policies_org_active ON expense_policies(organization_id, is_active);
