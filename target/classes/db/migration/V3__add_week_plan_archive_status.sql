ALTER TABLE week_plans ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE week_plans ADD COLUMN archived_at TIMESTAMP NULL;

CREATE INDEX idx_week_plans_user_status ON week_plans(user_id, status);
CREATE INDEX idx_week_plans_status_year_week ON week_plans(status, year_number, week_number);
