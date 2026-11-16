package com.weeklyplan.plan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeekPlanParticipantRepository extends JpaRepository<WeekPlanParticipant, WeekPlanParticipantId> {
  boolean existsByPlanIdAndUserId(Long planId, Long userId);
  void deleteByPlanIdAndUserId(Long planId, Long userId);
}
