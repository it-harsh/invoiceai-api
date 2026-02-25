CREATE TABLE budget_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    budget_id       UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    alert_type      VARCHAR(50) NOT NULL,
    month           DATE NOT NULL,
    actual_amount   DECIMAL(12, 2) NOT NULL,
    budget_amount   DECIMAL(12, 2) NOT NULL,
    percentage      DECIMAL(5, 2) NOT NULL,
    notified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budget_alerts_org ON budget_alerts(organization_id);
CREATE UNIQUE INDEX idx_budget_alerts_unique ON budget_alerts(budget_id, alert_type, month);
