-- 注册先进入待审核态；新公司申请和已有公司入职申请共用审核记录。
INSERT INTO roles (code, name) SELECT 'SUPER_ADMIN', '超级管理员' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SUPER_ADMIN');
CREATE TABLE registration_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  company_id BIGINT NULL,
  new_company_name VARCHAR(128) NULL,
  new_company_code VARCHAR(64) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  reviewed_by_user_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL,
  CONSTRAINT fk_registration_request_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_registration_request_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_registration_request_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id),
  INDEX idx_registration_request_status (status)
);
