package com.weeklyplan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
    @NotBlank @Size(min = 2, max = 30) String displayName,
    @NotBlank @Size(min = 8, max = 72) String currentPassword,
    @Size(min = 8, max = 72) String newPassword
) {}
