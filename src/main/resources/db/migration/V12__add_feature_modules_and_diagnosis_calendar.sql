CREATE TABLE feature_modules (
  code VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_feature_modules (
  company_id BIGINT NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  enabled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (company_id, module_code),
  CONSTRAINT fk_company_feature_modules_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT fk_company_feature_modules_module FOREIGN KEY (module_code) REFERENCES feature_modules(code) ON DELETE CASCADE
);

CREATE TABLE user_feature_modules (
  user_id BIGINT NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  enabled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, module_code),
  CONSTRAINT fk_user_feature_modules_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_feature_modules_module FOREIGN KEY (module_code) REFERENCES feature_modules(code) ON DELETE CASCADE
);

INSERT INTO feature_modules (code, name, description) VALUES
  ('TEAM_DIAGNOSIS_CALENDAR', '团队诊断日历', '按月统筹企业诊断工作与参与人员');

CREATE TABLE diagnosis_works (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  work_date DATE NOT NULL,
  enterprise_name VARCHAR(128) NOT NULL,
  address VARCHAR(255) NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_diagnosis_works_company_date (company_id, work_date),
  CONSTRAINT fk_diagnosis_works_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT fk_diagnosis_works_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE diagnosis_work_participants (
  diagnosis_work_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (diagnosis_work_id, user_id),
  CONSTRAINT fk_diagnosis_work_participants_work FOREIGN KEY (diagnosis_work_id) REFERENCES diagnosis_works(id) ON DELETE CASCADE,
  CONSTRAINT fk_diagnosis_work_participants_user FOREIGN KEY (user_id) REFERENCES users(id)
);
