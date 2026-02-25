CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    category_id     UUID REFERENCES categories(id) ON DELETE CASCADE,
    monthly_limit   DECIMAL(12, 2) NOT NULL,
    alert_at_80     BOOLEAN NOT NULL DEFAULT TRUE,
    alert_at_100    BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budgets_org ON budgets(organization_id);
CREATE UNIQUE INDEX idx_budgets_org_overall ON budgets(organization_id) WHERE category_id IS NULL AND is_active = TRUE;
CREATE UNIQUE INDEX idx_budgets_org_category ON budgets(organization_id, category_id) WHERE category_id IS NOT NULL AND is_active = TRUE;
