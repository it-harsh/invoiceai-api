CREATE TABLE policy_violations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    expense_id      UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    policy_id       UUID NOT NULL REFERENCES expense_policies(id) ON DELETE CASCADE,
    violation_message TEXT NOT NULL,
    notified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_violations_org ON policy_violations(organization_id);
CREATE INDEX idx_violations_expense ON policy_violations(expense_id);
