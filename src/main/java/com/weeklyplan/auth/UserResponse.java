package com.weeklyplan.auth;

import com.weeklyplan.user.AppUser;
import java.time.Instant;

public record UserResponse(String id, String userCode, String username, String displayName, String role, Instant createdAt) {
  public static UserResponse of(AppUser user) {
    return new UserResponse(user.getId().toString(), user.getUserCode(), user.getUsername(), user.getDisplayName(), user.getRole().getCode().toLowerCase(), user.getCreatedAt());
  }
}
