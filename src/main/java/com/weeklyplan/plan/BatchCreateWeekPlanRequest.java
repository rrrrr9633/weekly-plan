package com.weeklyplan.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchCreateWeekPlanRequest(
    @NotNull Long projectId,
    @Min(2000) @Max(2100) int year,
    @Min(1) @Max(53) int weekNumber,
    @NotEmpty List<@Valid PlanItem> plans
) {
  public record PlanItem(
      @NotBlank @Size(max = 4000) String content,
      @NotNull PlanWeekday weekday
  ) {}
}
