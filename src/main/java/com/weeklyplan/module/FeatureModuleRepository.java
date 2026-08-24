package com.weeklyplan.module;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeatureModuleRepository extends JpaRepository<FeatureModule, String> {
  List<FeatureModule> findAllByOrderByNameAsc();
}
