ALTER TABLE week_plans ADD COLUMN board_position INT NULL;
CREATE INDEX idx_week_plans_board_order ON week_plans (project_id, year_number, week_number, weekday, board_position);
