package com.weeklyplan.partner;
import jakarta.validation.constraints.NotNull;
public record SavePartnerProjectRequest(@NotNull Long projectId, String cooperationRole, String note) {}
