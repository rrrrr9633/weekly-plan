package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AiDtos {
  private AiDtos() {}
  public record ConversationRequest(@NotBlank String message, Long conversationId) {}
  public record ConversationCreateRequest(String title) {}
  public record SupplementRequest(java.util.Map<String, String> fields) {}
  public record ModelProposal(String operationType, JsonNode payload, String preview) {}
  public record ProposalResponse(Long id, String operationType, String status, String preview, String message, String error, Object missingFields, Long conversationId) {}
  public record ConversationSummary(Long id, String title, Instant createdAt, Instant updatedAt, Instant expiresAt) {}
  public record MessageResponse(Long id, String sender, String content, Long proposalId, Instant createdAt) {}
  public record ChatMessage(String role, String content) {}
  public record ConversationDetail(ConversationSummary conversation, java.util.List<MessageResponse> messages) {}
  public record MemoryResponse(Long id, Long actorUserId, String actorDisplayName, String operationType, String summary, Instant createdAt) {}
}
