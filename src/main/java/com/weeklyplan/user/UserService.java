package com.weeklyplan.user;

import com.weeklyplan.auth.UserResponse;
import com.weeklyplan.plan.WeekPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {
  private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder passwords; private final WeekPlanRepository plans;
  public UserService(UserRepository users, RoleRepository roles, PasswordEncoder passwords, WeekPlanRepository plans) { this.users = users; this.roles = roles; this.passwords = passwords; this.plans = plans; }
  public List<UserResponse> list() { return users.findAll().stream().map(UserResponse::of).toList(); }
  public UserResponse create(CreateUserRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    Role role = resolveRole(request.role());
    AppUser user = users.save(AppUser.create("U" + System.currentTimeMillis(), username, passwords.encode(request.password()), request.username().trim(), role));
    return UserResponse.of(user);
  }

  public UserResponse update(Long id, UpdateUserRequest request) {
    AppUser user = requireUser(id);
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsernameAndIdNot(username, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    user.update(username, request.username().trim(), resolveRole(request.role()));
    return UserResponse.of(user);
  }

  public void delete(Long id, String currentUserId) {
    if (id.equals(parseId(currentUserId))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除当前登录用户");
    if (plans.existsByUserId(id) || plans.existsByAssignedById(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该用户已有周计划关联记录，不能删除");
    }
    users.delete(requireUser(id));
  }

  private AppUser requireUser(Long id) {
    return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
  }

  private Role resolveRole(String role) {
    String roleCode = "admin".equals(role) ? "ADMIN" : "USER";
    return roles.findByCode(roleCode).orElseThrow();
  }

  private Long parseId(String value) {
    try { return Long.valueOf(value); }
    catch (NumberFormatException error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"); }
  }
}
