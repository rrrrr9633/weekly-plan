package com.weeklyplan.project;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String code;
  @Column(nullable = false) private String name;
  private String description;
  @Column(name = "assist_org") private String assistOrg;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private ProjectStatus status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getAssistOrg() { return assistOrg; }
  public ProjectStatus getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }

  public static Project create(String name, String code, String description, String assistOrg) {
    Project project = new Project();
    project.name = name; project.code = code; project.description = description; project.assistOrg = assistOrg;
    project.status = ProjectStatus.ACTIVE; project.createdAt = Instant.now(); project.updatedAt = Instant.now();
    return project;
  }

  public void update(String name, String description, String assistOrg, ProjectStatus status) {
    if (name != null) this.name = name;
    if (description != null) this.description = description;
    if (assistOrg != null) this.assistOrg = assistOrg;
    if (status != null) this.status = status;
    this.updatedAt = Instant.now();
  }
}
