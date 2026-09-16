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
  private final UserRepository users; private final RoleRepository roles; private final CompanyRepository companies; private final RegistrationRequestRepository requests; private final PasswordEncoder passwords; private final JwtService jwt;
  public AuthService(UserRepository users, RoleRepository roles, CompanyRepository companies, RegistrationRequestRepository requests, PasswordEncoder passwords, JwtService jwt) { this.users = users; this.roles = roles; this.companies = companies; this.requests = requests; this.passwords = passwords; this.jwt = jwt; }
  public AuthResponse register(AuthRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (users.existsByUsername(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
    boolean newCompany = request.newCompanyName() != null && !request.newCompanyName().isBlank();
    if (newCompany && (request.newCompanyCode() == null || request.newCompanyCode().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新公司必须填写公司编码");
    Company company = null;
    if (!newCompany) {
      if (request.companyId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属公司或申请注册新公司");
      company = companies.findById(request.companyId()).filter(item -> "ACTIVE".equals(item.getStatus())).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司不存在或已停用"));
    } else if (companies.findByCode(request.newCompanyCode().trim().toUpperCase(Locale.ROOT)).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "公司编码已存在");
    }
    Role role = roles.findByCode("USER").orElseThrow();
    AppUser user = AppUser.create("U" + System.currentTimeMillis(), username, passwords.encode(request.password()), request.username().trim(), role);
    user.setCompany(company); user.setStatus(UserStatus.PENDING); user = users.save(user);
    requests.save(newCompany ? RegistrationRequest.newCompany(user, request.newCompanyName().trim(), request.newCompanyCode().trim().toUpperCase(Locale.ROOT)) : RegistrationRequest.existing(user, company));
    return AuthResponse.pending(user);
  }
  public AuthResponse login(AuthRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    AppUser user = users.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不存在"));
    if (user.getStatus() == UserStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "注册申请正在审核中，请等待管理员通过");
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用，请联系管理员");
    }
    if (!passwords.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误，请重新输入");
    }
    user.setLastLoginAt(Instant.now()); users.save(user); return AuthResponse.of(jwt.createAccessToken(user), user);
  }
}
