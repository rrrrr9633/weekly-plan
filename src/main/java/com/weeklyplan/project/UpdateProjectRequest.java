package com.weeklyplan.project;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(max = 128) String name,
    @Size(max = 2000) String description,
    @Size(max = 128) String assistOrg,
    String status,
    Boolean hidden
) {}
