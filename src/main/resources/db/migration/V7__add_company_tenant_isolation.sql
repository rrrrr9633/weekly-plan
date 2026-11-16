CREATE TABLE companies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (code, name)
VALUES ('SUPER_ADMIN', '超级管理员');

INSERT INTO companies (code, name)
VALUES ('LIAONING_GUQI_DATA', '辽宁谷器数据');

ALTER TABLE users ADD COLUMN company_id BIGINT NULL;
ALTER TABLE projects ADD COLUMN company_id BIGINT NULL;

UPDATE users
SET company_id = (SELECT id FROM companies WHERE code = 'LIAONING_GUQI_DATA')
WHERE company_id IS NULL;

UPDATE projects
SET company_id = (SELECT id FROM companies WHERE code = 'LIAONING_GUQI_DATA')
WHERE company_id IS NULL;

ALTER TABLE projects MODIFY COLUMN company_id BIGINT NOT NULL;

ALTER TABLE users
  ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies(id);
ALTER TABLE projects
  ADD CONSTRAINT fk_projects_company FOREIGN KEY (company_id) REFERENCES companies(id);

ALTER TABLE projects DROP INDEX code;
CREATE UNIQUE INDEX uk_projects_company_code ON projects(company_id, code);

CREATE INDEX idx_users_company_status ON users(company_id, status);
CREATE INDEX idx_projects_company_status ON projects(company_id, status);
