package com.weeklyplan.plan;

import java.time.Instant;
import java.time.LocalDate;

public record WeekPlanResponse(
    String id, String projectId, String projectName, String projectCode,
    String userId, String username, String displayName, int year, int weekNumber, String weekday,
    LocalDate weekStart, LocalDate weekEnd, String content,
    String assignedBy, String assignedByUserId, boolean isAssigned, String status, Instant archivedAt, Long boardPosition, Instant createdAt, Instant updatedAt
) {}
