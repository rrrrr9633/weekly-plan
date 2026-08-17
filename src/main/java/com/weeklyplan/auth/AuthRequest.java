package com.weeklyplan.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AuthRequest(
  @NotBlank @Size(max = 64) String username,
  @NotBlank @Size(min = 8, max = 72) String password,
  @Positive Long companyId
) {}
