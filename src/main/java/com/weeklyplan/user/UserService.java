package com.weeklyplan.user;

import com.weeklyplan.auth.UserResponse;
import com.weeklyplan.company.Company;
import com.weeklyplan.company.CompanyRepository;
import com.weeklyplan.plan.WeekPlanRepository;
import com.weeklyplan.tenant.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {
  private final UserRepository users; private final RoleRepository roles; private final CompanyRepository companies; private final PasswordEncoder passwords; private final WeekPlanRepository plans; private final TenantAccessService tenant;
  public UserService(UserRepository users, RoleRepository roles, CompanyRepository companies, PasswordEncoder passwords, WeekPlanRepository plans, TenantAccessService tenant) { this.users = users; this.roles = roles; this.companies = companies; this.passwords = passwords; this.plans = plans; this.tenant = tenant; }
  public List<UserResponse> list() {
    return users.findByCompanyId(tenant.currentCompany().getId()).stream().map(UserResponse::of).toList();
  }
  public UserResponse create(CreateUserRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    Role role = resolveRole(request.role());
    AppUser user = AppUser.create("U" + System.currentTimeMillis(), username, passwords.encode(request.password()), request.username().trim(), role);
    user.setCompany(tenant.currentCompany());
    user = users.save(user);
    return UserResponse.of(user);
  }

  public UserResponse update(Long id, UpdateUserRequest request) {
    AppUser user = requireManagedUser(id);
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsernameAndIdNot(username, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    user.update(username, request.username().trim(), resolveRole(request.role()));
    users.save(user);
    return UserResponse.of(user);
  }

  public UserResponse moveToCompany(Long id, MoveUserCompanyRequest request, String currentUserId) {
    if (!tenant.isSuperAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅超级管理员可调整用户所属公司");
    if (id.equals(parseId(currentUserId))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能调整当前登录超级管理员的公司归属");
    AppUser user = requireUser(id);
    if ("SUPER_ADMIN".equals(user.getRole().getCode())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能调整超级管理员的公司归属");
    if (plans.existsByUserId(id) || plans.existsByAssignedById(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该用户已有计划记录，不能直接切换公司");
    }
    Company company = companies.findById(request.companyId())
        .filter(item -> "ACTIVE".equals(item.getStatus()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司不存在或已停用"));
    user.setCompany(company);
    return UserResponse.of(users.save(user));
  }

  public UserResponse getMyProfile(String currentUserId) {
    return UserResponse.of(requireUser(parseId(currentUserId)));
  }

  public UserResponse updateMyProfile(String currentUserId, UpdateMyProfileRequest request) {
    AppUser user = requireUser(parseId(currentUserId));
    String displayName = request.displayName().trim();
    if (displayName.length() < 2 || displayName.length() > 30) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名长度应为 2–30 个字符");
    }
    user.update(user.getUsername(), displayName, user.getRole());
    users.save(user);
    return UserResponse.of(user);
  }

  public void updateMyPassword(String currentUserId, UpdatePasswordRequest request) {
    AppUser user = requireUser(parseId(currentUserId));
    if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前密码不正确");
    }
    user.updatePassword(passwords.encode(request.newPassword()));
    users.save(user);
  }

  public void delete(Long id, String currentUserId) {
    if (id.equals(parseId(currentUserId))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除当前登录用户");
    if (plans.existsByUserId(id) || plans.existsByAssignedById(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该用户已有周计划关联记录，不能删除");
    }
    users.delete(requireManagedUser(id));
  }

  private AppUser requireManagedUser(Long id) {
    AppUser user = requireUser(id);
    tenant.assertCompany(user);
    return user;
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
