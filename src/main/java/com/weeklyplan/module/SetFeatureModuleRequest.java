package com.weeklyplan.module;

import jakarta.validation.constraints.NotNull;

public record SetFeatureModuleRequest(@NotNull Boolean enabled) {}
