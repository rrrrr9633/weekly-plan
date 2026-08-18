package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public final class AiDtos {
  private AiDtos() {}
  public record ConversationRequest(@NotBlank String message) {}
  public record ModelProposal(String operationType, JsonNode payload, String preview) {}
  /** Deliberately contains no model payload or raw query rows: those are implementation details, not chat text. */
  public record ProposalResponse(Long id, String operationType, String status, String preview, String message, String error) {}
}
