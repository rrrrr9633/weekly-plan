package com.weeklyplan.partner;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record SavePartnerResourceRequest(@NotBlank @Size(max=128) String name, @Size(max=255) String website, @Size(max=128) String region, String introduction, @NotBlank String cooperationStatus, @NotNull List<@NotBlank String> roles, @NotNull List<@NotBlank String> tags, LocalDate nextFollowUpDate, boolean preferred, @Size(max=500) String riskNote, @Min(1) @Max(5) Integer technicalScore, @Min(1) @Max(5) Integer commercialScore, @Min(1) @Max(5) Integer deliveryScore) {}
