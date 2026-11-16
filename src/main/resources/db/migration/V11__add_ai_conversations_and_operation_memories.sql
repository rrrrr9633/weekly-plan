CREATE TABLE ai_conversations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_ai_conversation_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_ai_conversation_user_expiry ON ai_conversations(user_id, expires_at);

CREATE TABLE ai_conversation_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  proposal_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_message_proposal FOREIGN KEY (proposal_id) REFERENCES ai_operation_proposals(id) ON DELETE SET NULL
);
CREATE INDEX idx_ai_message_conversation_created ON ai_conversation_messages(conversation_id, created_at);

CREATE TABLE ai_operation_memories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  proposal_id BIGINT NOT NULL,
  operation_type VARCHAR(32) NOT NULL,
  summary TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_memory_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_ai_memory_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
  CONSTRAINT fk_ai_memory_proposal FOREIGN KEY (proposal_id) REFERENCES ai_operation_proposals(id) ON DELETE CASCADE,
  CONSTRAINT uq_ai_memory_proposal UNIQUE (proposal_id)
);
CREATE INDEX idx_ai_memory_company_created ON ai_operation_memories(company_id, created_at);
