package com.weeklyplan.diagnosis;

import java.time.LocalDate;
import java.util.List;

public record ImportedDiagnosisRow(LocalDate workDate, String enterpriseName, String county, String diagnosisTime, Integer diagnosisRound, String enterpriseContact, String enterpriseContactPhone, List<String> participantNames) {}
