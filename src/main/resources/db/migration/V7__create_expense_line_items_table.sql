CREATE TABLE expense_line_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id  UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity    DECIMAL(10, 2) NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12, 2) NOT NULL,
    total       DECIMAL(12, 2) NOT NULL
);

CREATE INDEX idx_line_items_expense ON expense_line_items(expense_id);
