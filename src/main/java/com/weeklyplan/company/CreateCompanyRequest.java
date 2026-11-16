package com.weeklyplan.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
  @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
  @NotBlank @Size(max = 128) String name
) {}
