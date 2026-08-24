package com.weeklyplan.module;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface UserFeatureModuleRepository extends JpaRepository<UserFeatureModule, UserFeatureModuleId> {
  List<UserFeatureModule> findByIdUserId(Long userId);
  boolean existsByIdUserIdAndIdModuleCode(Long userId, String moduleCode);
}
