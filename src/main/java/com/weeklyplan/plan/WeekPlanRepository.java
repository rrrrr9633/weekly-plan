package com.weeklyplan.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {
  List<WeekPlan> findByUserIdAndYearAndWeekNumberOrderByCreatedAtAsc(Long userId, int year, int weekNumber);
  List<WeekPlan> findByUserIdAndYearAndWeekNumberAndStatusOrderByCreatedAtDesc(Long userId, int year, int weekNumber, PlanStatus status);
  List<WeekPlan> findByYearAndWeekNumberOrderByUserDisplayNameAscCreatedAtDesc(int year, int weekNumber);
  List<WeekPlan> findByProjectIdAndYearAndWeekNumberAndWeekday(Long projectId, int year, int weekNumber, PlanWeekday weekday);
  List<WeekPlan> findByUserIdAndStatusOrderByArchivedAtDesc(Long userId, PlanStatus status);
  Optional<WeekPlan> findByIdAndUserId(Long id, Long userId);
  boolean existsByUserId(Long userId);
  boolean existsByAssignedById(Long userId);
}
