package com.weeklyplan.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {
  List<WeekPlan> findByUserIdAndYearAndWeekNumberOrderByCreatedAtAsc(Long userId, int year, int weekNumber);
  List<WeekPlan> findByUserIdAndYearAndWeekNumberAndStatusOrderByCreatedAtDesc(Long userId, int year, int weekNumber, PlanStatus status);
  List<WeekPlan> findByYearAndWeekNumberOrderByUserDisplayNameAscCreatedAtDesc(int year, int weekNumber);
  List<WeekPlan> findByProjectIdAndYearAndWeekNumberAndWeekday(Long projectId, int year, int weekNumber, PlanWeekday weekday);
  List<WeekPlan> findByUserIdAndStatusOrderByArchivedAtDesc(Long userId, PlanStatus status);
  @Query("select distinct p from WeekPlan p join p.participants participant where participant.user.id = :userId and p.year = :year and p.weekNumber = :week")
  List<WeekPlan> findParticipatingByUserAndWeek(@Param("userId") Long userId, @Param("year") int year, @Param("week") int week);
  @Query("select distinct p from WeekPlan p join p.participants participant where participant.user.id = :userId and p.year = :year and p.weekNumber = :week and p.project.id in :projectIds")
  List<WeekPlan> findParticipatingByUserAndWeekAndProjectIdIn(@Param("userId") Long userId, @Param("year") int year, @Param("week") int week, @Param("projectIds") List<Long> projectIds);
  @Query("select distinct p from WeekPlan p join p.participants participant where participant.user.id = :userId and p.year = :year and p.weekNumber = :week and p.status = :status")
  List<WeekPlan> findParticipatingByUserAndWeekAndStatus(@Param("userId") Long userId, @Param("year") int year, @Param("week") int week, @Param("status") PlanStatus status);
  @Query("select distinct p from WeekPlan p join p.participants participant where participant.user.id = :userId and p.status = :status order by p.archivedAt desc")
  List<WeekPlan> findParticipatingByUserAndStatus(@Param("userId") Long userId, @Param("status") PlanStatus status);
  @Query("select distinct p from WeekPlan p join p.participants participant where participant.user.id = :userId order by p.year desc, p.weekNumber desc, p.createdAt desc")
  List<WeekPlan> findAllParticipatingByUser(@Param("userId") Long userId);
  Optional<WeekPlan> findByIdAndUserId(Long id, Long userId);
  boolean existsByUserId(Long userId);
  boolean existsByAssignedById(Long userId);
}
