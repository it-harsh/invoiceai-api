CREATE TABLE expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invoice_id      UUID REFERENCES invoices(id) ON DELETE SET NULL,
    category_id     UUID REFERENCES categories(id) ON DELETE SET NULL,
    vendor_name     VARCHAR(255) NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    tax_amount      DECIMAL(12, 2) DEFAULT 0,
    date            DATE NOT NULL,
    description     TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'NEEDS_REVIEW',
    ai_confidence   DECIMAL(3, 2),
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_org ON expenses(organization_id);
CREATE INDEX idx_expenses_org_date ON expenses(organization_id, date DESC);
CREATE INDEX idx_expenses_org_category ON expenses(organization_id, category_id);
CREATE INDEX idx_expenses_org_status ON expenses(organization_id, status);
CREATE INDEX idx_expenses_invoice ON expenses(invoice_id);
