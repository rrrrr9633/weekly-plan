package com.weeklyplan.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
  List<AiConversation> findByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(Long userId, Instant now);
  Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
  long deleteByExpiresAtBefore(Instant cutoff);
}
