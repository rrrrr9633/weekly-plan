package com.weeklyplan.user;

import com.weeklyplan.auth.UserResponse;
import com.weeklyplan.auth.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/users")
public class UserController {
  private final UserService users;
  public UserController(UserService users) { this.users = users; }
  @GetMapping public List<UserResponse> list() { return users.list(); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return users.create(request); }
  @PutMapping("/me") public AuthResponse updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateMyProfileRequest request) { return users.updateMyProfile(authentication.getName(), request); }
  @PutMapping("/{id}") public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) { return users.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication authentication, @PathVariable Long id) { users.delete(id, authentication.getName()); }
}
