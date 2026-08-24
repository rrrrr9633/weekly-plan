package com.weeklyplan.module;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class UserFeatureModuleId implements Serializable {
  @Column(name = "user_id") private Long userId;
  @Column(name = "module_code") private String moduleCode;
  protected UserFeatureModuleId() {}
  UserFeatureModuleId(Long userId, String moduleCode) { this.userId = userId; this.moduleCode = moduleCode; }
  String moduleCode() { return moduleCode; }
  @Override public boolean equals(Object value) { return value instanceof UserFeatureModuleId other && Objects.equals(userId, other.userId) && Objects.equals(moduleCode, other.moduleCode); }
  @Override public int hashCode() { return Objects.hash(userId, moduleCode); }
}

@Entity
@Table(name = "user_feature_modules")
class UserFeatureModule {
  @EmbeddedId private UserFeatureModuleId id;
  protected UserFeatureModule() {}
  UserFeatureModule(Long userId, String moduleCode) { this.id = new UserFeatureModuleId(userId, moduleCode); }
  String moduleCode() { return id.moduleCode(); }
}
