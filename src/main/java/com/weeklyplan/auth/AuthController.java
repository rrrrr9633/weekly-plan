package com.weeklyplan.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth")
public class AuthController {
  private final AuthService auth;
  public AuthController(AuthService auth) { this.auth = auth; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthResponse register(@Valid @RequestBody AuthRequest request) { return auth.register(request); }
  @PostMapping("/login") public AuthResponse login(@Valid @RequestBody AuthRequest request) { return auth.login(request); }
}
