package com.weeklyplan.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoveUserCompanyRequest(@NotNull @Positive Long companyId) {}
