package com.weeklyplan.diagnosis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

interface DiagnosisWorkRepository extends JpaRepository<DiagnosisWork, Long> {
  List<DiagnosisWork> findByCompanyIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(Long companyId, LocalDate start, LocalDate end);
  List<DiagnosisWork> findByCompanyIdAndDiagnosisRound(Long companyId, Integer diagnosisRound);

  @Query("""
    select distinct work from DiagnosisWork work
    left join work.participants participant
    where work.company.id = :companyId
      and work.workDate between :start and :end
      and (:enterprise is null or lower(work.enterpriseName) like lower(concat('%', :enterprise, '%')) escape '\\')
      and (
        :participant is null
        or lower(participant.displayName) like lower(concat('%', :participant, '%')) escape '\\'
        or lower(participant.username) like lower(concat('%', :participant, '%')) escape '\\'
      )
    order by work.workDate asc, work.createdAt asc
    """)
  List<DiagnosisWork> search(
    @Param("companyId") Long companyId,
    @Param("start") LocalDate start,
    @Param("end") LocalDate end,
    @Param("enterprise") String enterprise,
    @Param("participant") String participant
  );
}
