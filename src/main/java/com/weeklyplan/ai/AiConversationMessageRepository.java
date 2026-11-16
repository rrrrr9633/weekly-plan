package com.weeklyplan.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {
  List<AiConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
