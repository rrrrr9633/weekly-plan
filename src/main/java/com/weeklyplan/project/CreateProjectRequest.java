package com.weeklyplan.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank @Size(max = 128) String name,
    @NotBlank @Size(max = 64) String code,
    @Size(max = 2000) String description,
    @Size(max = 128) String assistOrg
) {}
