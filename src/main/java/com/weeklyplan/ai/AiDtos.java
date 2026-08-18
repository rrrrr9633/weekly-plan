package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public final class AiDtos {
  private AiDtos() {}
  public record ConversationRequest(@NotBlank String message) {}
  public record ModelProposal(String operationType, JsonNode payload, String preview) {}
  public record ProposalResponse(Long id, String operationType, String status, JsonNode payload, String preview, JsonNode result, String error, Object missingFields) {}
}
