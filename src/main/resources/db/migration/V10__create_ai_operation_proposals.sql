CREATE TABLE ai_operation_proposals (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  requested_by_user_id BIGINT NOT NULL,
  operation_type VARCHAR(32) NOT NULL,
  status VARCHAR(24) NOT NULL,
  payload TEXT NOT NULL,
  preview TEXT,
  result_json TEXT,
  error_message TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP NULL,
  executed_at TIMESTAMP NULL,
  CONSTRAINT fk_ai_proposal_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_ai_proposal_user FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);
CREATE INDEX idx_ai_proposal_user_created ON ai_operation_proposals(requested_by_user_id, created_at);
CREATE INDEX idx_ai_proposal_company_status ON ai_operation_proposals(company_id, status);
