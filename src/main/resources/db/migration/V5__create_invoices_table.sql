CREATE TABLE invoices (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    file_key                VARCHAR(500) NOT NULL,
    file_name               VARCHAR(255) NOT NULL,
    file_size               BIGINT NOT NULL,
    file_type               VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    uploaded_by             UUID NOT NULL REFERENCES users(id),
    processing_started_at   TIMESTAMPTZ,
    processing_completed_at TIMESTAMPTZ,
    ai_raw_response         JSONB,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_org ON invoices(organization_id);
CREATE INDEX idx_invoices_status ON invoices(organization_id, status);
CREATE INDEX idx_invoices_created ON invoices(organization_id, created_at DESC);
