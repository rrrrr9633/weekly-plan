package com.weeklyplan.user;

import com.weeklyplan.auth.UserResponse;
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
  @GetMapping("/me") public UserResponse getMyProfile(Authentication authentication) { return users.getMyProfile(authentication.getName()); }
  @PutMapping("/me") public UserResponse updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateMyProfileRequest request) { return users.updateMyProfile(authentication.getName(), request); }
  @PutMapping("/me/password") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateMyPassword(Authentication authentication, @Valid @RequestBody UpdatePasswordRequest request) { users.updateMyPassword(authentication.getName(), request); }
  @PutMapping("/{id}/company")
  public UserResponse moveToCompany(Authentication authentication, @PathVariable Long id, @Valid @RequestBody MoveUserCompanyRequest request) { return users.moveToCompany(id, request, authentication.getName()); }
  @PutMapping("/{id}") public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) { return users.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication authentication, @PathVariable Long id) { users.delete(id, authentication.getName()); }
}
