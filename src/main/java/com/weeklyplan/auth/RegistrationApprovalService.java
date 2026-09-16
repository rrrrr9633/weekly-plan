package com.weeklyplan.auth;

import com.weeklyplan.company.Company;
import com.weeklyplan.company.CompanyRepository;
import com.weeklyplan.tenant.TenantAccessService;
import com.weeklyplan.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class RegistrationApprovalService {
  private final RegistrationRequestRepository requests; private final UserRepository users; private final CompanyRepository companies; private final RoleRepository roles; private final TenantAccessService tenant;
  public RegistrationApprovalService(RegistrationRequestRepository requests, UserRepository users, CompanyRepository companies, RoleRepository roles, TenantAccessService tenant) { this.requests = requests; this.users = users; this.companies = companies; this.roles = roles; this.tenant = tenant; }
  @Transactional(readOnly = true)
  public List<RegistrationRequestResponse> pending() {
    List<RegistrationRequest> all = requests.findByStatusOrderByCreatedAtAsc("PENDING");
    if (tenant.isSuperAdmin()) return all.stream().map(RegistrationRequestResponse::of).toList();
    Long companyId = tenant.currentCompany().getId();
    return all.stream().filter(item -> item.getCompany() != null && companyId.equals(item.getCompany().getId())).map(RegistrationRequestResponse::of).toList();
  }
  @Transactional
  public RegistrationRequestResponse review(Long id, ReviewRegistrationRequest command) {
    RegistrationRequest request = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "注册申请不存在"));
    if (!"PENDING".equals(request.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "该申请已经处理");
    if (!tenant.isSuperAdmin() && (request.getCompany() == null || !tenant.currentCompany().getId().equals(request.getCompany().getId()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权审核该申请");
    AppUser reviewer = tenant.currentUser();
    if ("reject".equalsIgnoreCase(command.action())) { request.reject(reviewer); request.getUser().setStatus(UserStatus.DISABLED); return RegistrationRequestResponse.of(requests.save(request)); }
    if (!"approve".equalsIgnoreCase(command.action())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核动作无效");
    if (request.getCompany() == null) {
      String code = request.getNewCompanyCode().trim().toUpperCase(Locale.ROOT);
      if (companies.findByCode(code).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "公司编码已存在");
      Company company = companies.save(Company.create(code, request.getNewCompanyName().trim()));
      request.getUser().setCompany(company); request.getUser().setRole(roles.findByCode("ADMIN").orElseThrow());
    }
    request.getUser().setStatus(UserStatus.ACTIVE); users.save(request.getUser()); request.approve(reviewer); return RegistrationRequestResponse.of(requests.save(request));
  }
}
