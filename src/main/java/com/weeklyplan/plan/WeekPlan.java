package com.weeklyplan.plan;

import com.weeklyplan.project.Project;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "week_plans")
public class WeekPlan {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id", nullable = false) private Project project;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by_user_id") private AppUser assignedBy;
  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true) private Set<WeekPlanParticipant> participants = new LinkedHashSet<>();
  @Column(name = "year_number", nullable = false) private int year;
  @Column(name = "week_number", nullable = false) private int weekNumber;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private PlanWeekday weekday;
  @Column(nullable = false) private String content;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private PlanStatus status;
  @Column(name = "archived_at") private Instant archivedAt;
  @Column(name = "board_position") private Integer boardPosition;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public Project getProject() { return project; }
  public AppUser getUser() { return user; }
  public AppUser getAssignedBy() { return assignedBy; }
  public Set<WeekPlanParticipant> getParticipants() { return participants; }
  public boolean hasParticipant(Long userId) { return participants.stream().anyMatch(participant -> participant.getUser().getId().equals(userId)); }
  public void addParticipant(AppUser user) { if (!hasParticipant(user.getId())) participants.add(new WeekPlanParticipant(this, user)); }
  public void removeParticipant(Long userId) { participants.removeIf(participant -> participant.getUser().getId().equals(userId)); }
  public int getYear() { return year; }
  public int getWeekNumber() { return weekNumber; }
  public PlanWeekday getWeekday() { return weekday; }
  public String getContent() { return content; }
  public PlanStatus getStatus() { return status; }
  public Instant getArchivedAt() { return archivedAt; }
  public Integer getBoardPosition() { return boardPosition; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public static WeekPlan create(Project project, AppUser user, AppUser assignedBy, int year, int weekNumber, PlanWeekday weekday, String content) {
    WeekPlan plan = new WeekPlan();
    plan.project = project; plan.user = user; plan.assignedBy = assignedBy; plan.year = year; plan.weekNumber = weekNumber; plan.weekday = weekday;
    plan.content = content; plan.status = PlanStatus.ACTIVE; plan.createdAt = Instant.now(); plan.updatedAt = Instant.now();
    plan.addParticipant(user);
    return plan;
  }

  public void update(String content, PlanWeekday weekday) { this.content = content; this.weekday = weekday; this.updatedAt = Instant.now(); }
  public void archive() { this.status = PlanStatus.ARCHIVED; this.archivedAt = Instant.now(); this.updatedAt = Instant.now(); }
  public void restore() { this.status = PlanStatus.ACTIVE; this.archivedAt = null; this.updatedAt = Instant.now(); }
  public void setBoardPosition(Integer boardPosition) { this.boardPosition = boardPosition; this.updatedAt = Instant.now(); }
}
