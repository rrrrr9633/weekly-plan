package com.weeklyplan.tenant;

import com.weeklyplan.company.Company;
import com.weeklyplan.company.CompanyRepository;
import com.weeklyplan.user.AppUser;
import com.weeklyplan.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantAccessService {
  private static final String COMPANY_HEADER = "X-Company-Id";

  private final UserRepository users;
  private final CompanyRepository companies;

  public TenantAccessService(UserRepository users, CompanyRepository companies) {
    this.users = users;
    this.companies = companies;
  }

  public AppUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效");
    }
    try {
      return users.findById(Long.valueOf(authentication.getName()))
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录用户不存在"));
    } catch (NumberFormatException error) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效");
    }
  }

  public boolean isSuperAdmin() {
    return "SUPER_ADMIN".equals(currentUser().getRole().getCode());
  }

  public Company currentCompany() {
    AppUser user = currentUser();
    if ("SUPER_ADMIN".equals(user.getRole().getCode())) {
      String value = currentRequest().getHeader(COMPANY_HEADER);
      if (value == null || value.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "超级管理员访问业务数据时必须指定 X-Company-Id");
      }
      try {
        return companies.findById(Long.valueOf(value))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公司不存在"));
      } catch (NumberFormatException error) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Company-Id 无效");
      }
    }
    if (user.getCompany() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户未绑定公司");
    }
    return user.getCompany();
  }

  public void assertCompany(Company company) {
    if (company == null || !company.getId().equals(currentCompany().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
    }
  }

  public void assertCompany(AppUser user) {
    assertCompany(user.getCompany());
  }

  private jakarta.servlet.http.HttpServletRequest currentRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法确定公司上下文");
    }
    return attributes.getRequest();
  }
}
