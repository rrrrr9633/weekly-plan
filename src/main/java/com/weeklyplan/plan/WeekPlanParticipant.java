package com.weeklyplan.plan;

import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "week_plan_participants")
@IdClass(WeekPlanParticipantId.class)
public class WeekPlanParticipant {
  @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id", nullable = false) private WeekPlan plan;
  @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
  @Column(name = "joined_at", nullable = false) private Instant joinedAt;

  protected WeekPlanParticipant() {}
  public WeekPlanParticipant(WeekPlan plan, AppUser user) { this.plan = plan; this.user = user; this.joinedAt = Instant.now(); }
  public AppUser getUser() { return user; }
}
