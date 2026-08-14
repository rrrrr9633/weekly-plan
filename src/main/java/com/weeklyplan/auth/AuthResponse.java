package com.weeklyplan.auth;

import com.weeklyplan.user.AppUser;

public record AuthResponse(String token, UserResponse user) {
  public static AuthResponse of(String token, AppUser user) { return new AuthResponse(token, UserResponse.of(user)); }
}
