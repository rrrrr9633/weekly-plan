package com.weeklyplan.partner;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record SavePartnerFollowUpRequest(@NotBlank String content, @NotNull LocalDate followUpDate) {}
