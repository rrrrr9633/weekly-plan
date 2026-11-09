package com.weeklyplan.diagnosis;

import java.time.LocalDate;
import java.util.List;

public record ImportedDiagnosisRow(
    LocalDate workDate,
    String enterpriseName,
    String county,
    String diagnosisTime,
    Integer diagnosisRound,
    String enterpriseContact,
    String enterpriseContactPhone,
    List<String> participantNames,
    String nameMatch,
    Integer similarity,
    String matchedEnterpriseName,
    boolean sameRoundExists,
    Existing existing
) {
  public record Existing(
      String id,
      LocalDate workDate,
      String enterpriseName,
      String county,
      String diagnosisTime,
      Integer diagnosisRound,
      String enterpriseContact,
      String enterpriseContactPhone,
      List<String> participantNames
  ) {}

  public ImportedDiagnosisRow(LocalDate workDate, String enterpriseName, String county, String diagnosisTime, Integer diagnosisRound, String enterpriseContact, String enterpriseContactPhone, List<String> participantNames) {
    this(workDate, enterpriseName, county, diagnosisTime, diagnosisRound, enterpriseContact, enterpriseContactPhone, participantNames, "新", 0, "", false, null);
  }

  public ImportedDiagnosisRow withMatch(String nameMatch, Integer similarity, String matchedEnterpriseName, boolean sameRoundExists, Existing existing) {
    return new ImportedDiagnosisRow(workDate, enterpriseName, county, diagnosisTime, diagnosisRound, enterpriseContact, enterpriseContactPhone, participantNames, nameMatch, similarity, matchedEnterpriseName, sameRoundExists, existing);
  }
}
