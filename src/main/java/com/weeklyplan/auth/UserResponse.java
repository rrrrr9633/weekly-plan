package com.weeklyplan.auth;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import java.time.Instant;

public record UserResponse(String id, String userCode, String username, String displayName, String role, String companyId, String companyName, Instant createdAt) {
  public static UserResponse of(AppUser user) {
    Company company = user.getCompany();
    return new UserResponse(user.getId().toString(), user.getUserCode(), user.getUsername(), user.getDisplayName(), user.getRole().getCode().toLowerCase(),
        company == null ? null : company.getId().toString(), company == null ? null : company.getName(), user.getCreatedAt());
  }
}
