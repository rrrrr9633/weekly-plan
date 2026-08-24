package com.weeklyplan.diagnosis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

interface DiagnosisWorkRepository extends JpaRepository<DiagnosisWork, Long> {
  List<DiagnosisWork> findByCompanyIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(Long companyId, LocalDate start, LocalDate end);
}
