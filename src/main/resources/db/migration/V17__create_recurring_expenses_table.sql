CREATE TABLE recurring_expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    source_expense_id UUID REFERENCES expenses(id) ON DELETE SET NULL,
    vendor_name     VARCHAR(255) NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    tax_amount      DECIMAL(12, 2) DEFAULT 0,
    description     TEXT,
    category_id     UUID REFERENCES categories(id) ON DELETE SET NULL,
    frequency       VARCHAR(20) NOT NULL,
    next_due_date   DATE NOT NULL,
    last_created_at TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recurring_org ON recurring_expenses(organization_id);
CREATE INDEX idx_recurring_due ON recurring_expenses(next_due_date, is_active);
