ALTER TABLE week_plans
  ADD COLUMN weekday VARCHAR(16) NOT NULL DEFAULT 'PENDING';

ALTER TABLE week_plans DROP INDEX uk_week_plans_user_project_week;

CREATE INDEX idx_week_plans_user_project_week ON week_plans(user_id, project_id, year_number, week_number);
