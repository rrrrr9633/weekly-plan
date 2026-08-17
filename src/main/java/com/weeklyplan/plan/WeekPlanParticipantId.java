package com.weeklyplan.plan;

import java.io.Serializable;
import java.util.Objects;

public class WeekPlanParticipantId implements Serializable {
  private Long plan;
  private Long user;
  public WeekPlanParticipantId() {}
  public WeekPlanParticipantId(Long plan, Long user) { this.plan = plan; this.user = user; }
  @Override public boolean equals(Object value) {
    if (this == value) return true;
    if (!(value instanceof WeekPlanParticipantId that)) return false;
    return Objects.equals(plan, that.plan) && Objects.equals(user, that.user);
  }
  @Override public int hashCode() { return Objects.hash(plan, user); }
}
