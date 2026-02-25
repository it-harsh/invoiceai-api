ALTER TABLE expenses ADD COLUMN is_duplicate BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE expenses ADD COLUMN duplicate_of_id UUID REFERENCES expenses(id) ON DELETE SET NULL;

CREATE INDEX idx_expenses_dup_check ON expenses(organization_id, vendor_name, amount, date);
