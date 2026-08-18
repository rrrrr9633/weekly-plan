package com.weeklyplan.ai;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
public class AiConversationExpiryJob {
  private final AiConversationRepository conversations;

  public AiConversationExpiryJob(AiConversationRepository conversations) { this.conversations = conversations; }

  @Scheduled(fixedDelayString = "${app.ai.conversation-cleanup-ms:3600000}")
  @Transactional
  public void deleteExpiredConversations() {
    conversations.deleteByExpiresAtBefore(Instant.now());
  }
}
