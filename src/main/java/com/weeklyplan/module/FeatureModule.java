package com.weeklyplan.module;

import com.weeklyplan.company.Company;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feature_modules")
public class FeatureModule {
  @Id private String code;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private String description;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  public String getCode() { return code; }
  public String getName() { return name; }
  public String getDescription() { return description; }
}
