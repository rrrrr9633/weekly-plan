CREATE TABLE week_plan_participants (
  plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id, user_id),
  CONSTRAINT fk_week_plan_participants_plan FOREIGN KEY (plan_id) REFERENCES week_plans(id) ON DELETE CASCADE,
  CONSTRAINT fk_week_plan_participants_user FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO week_plan_participants (plan_id, user_id)
SELECT id, user_id FROM week_plans;

CREATE INDEX idx_week_plan_participants_user ON week_plan_participants (user_id, plan_id);
