package com.weeklyplan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
  @NotBlank @Size(max = 64) String username,
  @NotBlank @Size(min = 8, max = 72) String password,
  @Pattern(regexp = "admin|user") String role
) {}
