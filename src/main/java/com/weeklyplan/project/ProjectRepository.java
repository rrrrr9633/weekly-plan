package com.weeklyplan.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
  boolean existsByCompanyId(Long companyId);
  boolean existsByCompanyIdAndCode(Long companyId, String code);
  java.util.List<Project> findByCompanyId(Long companyId);
}
