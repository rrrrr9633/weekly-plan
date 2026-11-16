package com.weeklyplan.diagnosis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ImportDiagnosisChoice(
    @NotNull LocalDate workDate,
    @NotBlank @Size(max = 128) String enterpriseName,
    @NotBlank @Size(max = 128) String county,
    @NotBlank String diagnosisTime,
    @NotNull Integer diagnosisRound,
    @Size(max = 128) String enterpriseContact,
    @Size(max = 64) String enterpriseContactPhone,
    List<String> participantNames,
    List<Long> participantIds,
    String keep
) {}
