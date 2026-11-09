package com.weeklyplan.diagnosis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ApplyDiagnosisImportRequest(@NotEmpty List<@Valid ImportDiagnosisChoice> rows) {}
