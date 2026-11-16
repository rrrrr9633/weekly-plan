package com.weeklyplan.auth;

import com.weeklyplan.user.AppUser;

public record AuthResponse(String token, UserResponse user, String status) {
  public static AuthResponse of(String token, AppUser user) { return new AuthResponse(token, UserResponse.of(user), "ACTIVE"); }
  public static AuthResponse pending(AppUser user) { return new AuthResponse(null, UserResponse.of(user), "PENDING"); }
}
