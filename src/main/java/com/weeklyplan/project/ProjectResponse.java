package com.weeklyplan.project;

import java.time.Instant;

public record ProjectResponse(String id, String name, String code, String description, String assistOrg, String status, Instant createdAt) {
  public static ProjectResponse of(Project project) {
    return new ProjectResponse(
        String.valueOf(project.getId()), project.getName(), project.getCode(), project.getDescription(),
        project.getAssistOrg(), project.getStatus().name().toLowerCase(), project.getCreatedAt());
  }
}
