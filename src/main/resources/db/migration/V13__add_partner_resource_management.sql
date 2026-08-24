INSERT INTO feature_modules (code, name, description) VALUES
  ('PARTNER_RESOURCE_MANAGEMENT', '资源管理系统', '管理外部合作方、联系人、合作跟进与项目关联');

CREATE TABLE partner_resources (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  website VARCHAR(255),
  region VARCHAR(128),
  introduction TEXT,
  cooperation_status VARCHAR(32) NOT NULL DEFAULT 'LEAD',
  owner_user_id BIGINT NOT NULL,
  next_follow_up_date DATE,
  preferred BOOLEAN NOT NULL DEFAULT FALSE,
  risk_note VARCHAR(500),
  technical_score INT, commercial_score INT, delivery_score INT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_partner_resources_company_status (company_id, cooperation_status),
  CONSTRAINT fk_partner_resources_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT fk_partner_resources_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);
CREATE TABLE partner_resource_roles (
  resource_id BIGINT NOT NULL, role_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (resource_id, role_code),
  CONSTRAINT fk_partner_resource_roles_resource FOREIGN KEY (resource_id) REFERENCES partner_resources(id) ON DELETE CASCADE
);
CREATE TABLE partner_resource_tags (
  resource_id BIGINT NOT NULL, tag VARCHAR(64) NOT NULL,
  PRIMARY KEY (resource_id, tag),
  CONSTRAINT fk_partner_resource_tags_resource FOREIGN KEY (resource_id) REFERENCES partner_resources(id) ON DELETE CASCADE
);
CREATE TABLE partner_resource_contacts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, resource_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL, title VARCHAR(64), phone VARCHAR(32), wechat VARCHAR(64), email VARCHAR(128),
  PRIMARY KEY (id), CONSTRAINT fk_partner_resource_contacts_resource FOREIGN KEY (resource_id) REFERENCES partner_resources(id) ON DELETE CASCADE
);
CREATE TABLE partner_resource_follow_ups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, resource_id BIGINT NOT NULL, content TEXT NOT NULL,
  follow_up_date DATE NOT NULL, created_by_user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_partner_resource_followups_resource FOREIGN KEY (resource_id) REFERENCES partner_resources(id) ON DELETE CASCADE,
  CONSTRAINT fk_partner_resource_followups_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);
CREATE TABLE partner_resource_projects (
  resource_id BIGINT NOT NULL, project_id BIGINT NOT NULL, cooperation_role VARCHAR(255), note TEXT,
  PRIMARY KEY (resource_id, project_id),
  CONSTRAINT fk_partner_resource_projects_resource FOREIGN KEY (resource_id) REFERENCES partner_resources(id) ON DELETE CASCADE,
  CONSTRAINT fk_partner_resource_projects_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
