package com.weeklyplan.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveBoardOrderRequest(
    @NotNull Long projectId,
    @Min(2000) @Max(2100) int year,
    @Min(1) @Max(53) int weekNumber,
    @NotNull PlanWeekday weekday,
    @NotEmpty List<@NotNull Long> planIds
) {}
