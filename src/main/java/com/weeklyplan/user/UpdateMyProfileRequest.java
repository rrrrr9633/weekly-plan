package com.weeklyplan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(min = 8, max = 72) String currentPassword,
    @Size(min = 8, max = 72) String newPassword
) {}
