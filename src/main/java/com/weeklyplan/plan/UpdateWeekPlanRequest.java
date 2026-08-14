package com.weeklyplan.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWeekPlanRequest(
    @NotBlank @Size(max = 4000) String content,
    @NotNull PlanWeekday weekday
) {}
