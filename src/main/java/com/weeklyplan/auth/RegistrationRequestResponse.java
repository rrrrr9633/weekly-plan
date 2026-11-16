package com.weeklyplan.auth;

import java.time.Instant;

public record RegistrationRequestResponse(Long id, String username, String displayName, String companyId, String companyName, String newCompanyName, String newCompanyCode, String status, Instant createdAt) {
  public static RegistrationRequestResponse of(RegistrationRequest request) {
    return new RegistrationRequestResponse(request.getId(), request.getUser().getUsername(), request.getUser().getDisplayName(), request.getCompany() == null ? null : request.getCompany().getId().toString(), request.getCompany() == null ? null : request.getCompany().getName(), request.getNewCompanyName(), request.getNewCompanyCode(), request.getStatus(), request.getCreatedAt());
  }
}
