package com.weeklyplan.module;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CompanyFeatureModuleRepository extends JpaRepository<CompanyFeatureModule, CompanyFeatureModuleId> {
  List<CompanyFeatureModule> findByIdCompanyId(Long companyId);
  boolean existsByIdCompanyIdAndIdModuleCode(Long companyId, String moduleCode);
}
