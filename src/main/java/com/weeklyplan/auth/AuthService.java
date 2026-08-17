package com.weeklyplan.auth;

import com.weeklyplan.company.Company;
import com.weeklyplan.company.CompanyRepository;
import com.weeklyplan.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
  private final UserRepository users; private final RoleRepository roles; private final CompanyRepository companies; private final PasswordEncoder passwords; private final JwtService jwt;
  public AuthService(UserRepository users, RoleRepository roles, CompanyRepository companies, PasswordEncoder passwords, JwtService jwt) { this.users = users; this.roles = roles; this.companies = companies; this.passwords = passwords; this.jwt = jwt; }
  public AuthResponse register(AuthRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    Company company = companies.findById(request.companyId())
        .filter(item -> "ACTIVE".equals(item.getStatus()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司不存在或已停用"));
    Role role = roles.findByCode("USER").orElseThrow();
    AppUser user = AppUser.create("U" + System.currentTimeMillis(), username, passwords.encode(request.password()), request.username().trim(), role);
    user.setCompany(company);
    user = users.save(user);
    return AuthResponse.of(jwt.createAccessToken(user), user);
  }
  public AuthResponse login(AuthRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    AppUser user = users.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在"));
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用，请联系管理员");
    }
    if (!passwords.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误，请重新输入");
    }
    user.setLastLoginAt(Instant.now()); users.save(user); return AuthResponse.of(jwt.createAccessToken(user), user);
  }
}
