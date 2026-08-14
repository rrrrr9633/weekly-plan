package com.weeklyplan.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignWeekPlanRequest(
    @NotNull Long projectId,
    @NotNull Long userId,
    @Min(2000) @Max(2100) int year,
    @Min(1) @Max(53) int weekNumber,
    @NotNull PlanWeekday weekday,
    @NotBlank @Size(max = 4000) String content
) {}
