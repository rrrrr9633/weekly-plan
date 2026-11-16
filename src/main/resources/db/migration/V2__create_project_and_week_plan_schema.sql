CREATE TABLE projects (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(2000),
  assist_org VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE week_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by_user_id BIGINT,
  year_number INT NOT NULL,
  week_number INT NOT NULL,
  content VARCHAR(4000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_week_plans_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_week_plans_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_week_plans_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id),
  CONSTRAINT uk_week_plans_user_project_week UNIQUE (user_id, project_id, year_number, week_number)
);

CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_week_plans_year_week ON week_plans(year_number, week_number);
CREATE INDEX idx_week_plans_user_year_week ON week_plans(user_id, year_number, week_number);
