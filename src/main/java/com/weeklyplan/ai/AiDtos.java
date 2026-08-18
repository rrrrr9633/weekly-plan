package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public final class AiDtos {
  private AiDtos() {}
  public record ConversationRequest(@NotBlank String message) {}
  public record ModelProposal(String operationType, JsonNode payload, String preview) {}
  /** The response is a UI contract: it never exposes model payloads or raw query rows. */
  public record ProposalResponse(Long id, String operationType, String status, String preview, String message, String error, Object missingFields) {}
}
