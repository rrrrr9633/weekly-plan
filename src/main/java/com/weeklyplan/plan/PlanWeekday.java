package com.weeklyplan.plan;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum PlanWeekday {
  MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, PENDING;

  @JsonCreator
  public static PlanWeekday fromValue(String value) {
    return PlanWeekday.valueOf(value.toUpperCase(Locale.ROOT));
  }
}
