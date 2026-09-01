package com.weeklyplan.diagnosis;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DiagnosisWorkResponse(String id, LocalDate workDate, String enterpriseName, String address, String county, String diagnosisTime, Integer diagnosisRound, String serviceProviderContact, String createdByName, List<Participant> participants, Instant createdAt, Instant updatedAt) {
  public record Participant(String id, String displayName) {}
}
