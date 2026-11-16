package com.weeklyplan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 2, max = 30) String displayName,
    @Pattern(regexp = "admin|user") String role
) {}
