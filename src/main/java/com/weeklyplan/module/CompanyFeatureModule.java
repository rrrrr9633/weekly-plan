package com.weeklyplan.module;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
class CompanyFeatureModuleId implements Serializable {
  @Column(name = "company_id") private Long companyId;
  @Column(name = "module_code") private String moduleCode;
  protected CompanyFeatureModuleId() {}
  CompanyFeatureModuleId(Long companyId, String moduleCode) { this.companyId = companyId; this.moduleCode = moduleCode; }
  String moduleCode() { return moduleCode; }
  @Override public boolean equals(Object value) { return value instanceof CompanyFeatureModuleId other && Objects.equals(companyId, other.companyId) && Objects.equals(moduleCode, other.moduleCode); }
  @Override public int hashCode() { return Objects.hash(companyId, moduleCode); }
}

@Entity
@Table(name = "company_feature_modules")
class CompanyFeatureModule {
  @EmbeddedId private CompanyFeatureModuleId id;
  protected CompanyFeatureModule() {}
  CompanyFeatureModule(Long companyId, String moduleCode) { this.id = new CompanyFeatureModuleId(companyId, moduleCode); }
  String moduleCode() { return id.moduleCode(); }
}
