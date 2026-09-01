package com.weeklyplan.diagnosis;

import java.time.LocalDate;

public record ImportedDiagnosisRow(LocalDate workDate, String enterpriseName, String county, String diagnosisTime, Integer diagnosisRound, String serviceProviderContact) {}
