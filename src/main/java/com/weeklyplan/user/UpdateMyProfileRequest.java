package com.weeklyplan.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
    @JsonAlias("username") @NotBlank @Size(min = 2, max = 30) String displayName
) {}
