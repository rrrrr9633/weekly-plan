package com.weeklyplan.module;

import com.weeklyplan.company.Company;
import com.weeklyplan.company.CompanyRepository;
import com.weeklyplan.tenant.TenantAccessService;
import com.weeklyplan.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@Service
public class FeatureModuleService {
  private final FeatureModuleRepository modules;
  private final CompanyFeatureModuleRepository companyModules;
  private final UserFeatureModuleRepository userModules;
  private final CompanyRepository companies;
  private final TenantAccessService tenant;
  public FeatureModuleService(FeatureModuleRepository modules, CompanyFeatureModuleRepository companyModules, UserFeatureModuleRepository userModules, CompanyRepository companies, TenantAccessService tenant) {
    this.modules = modules; this.companyModules = companyModules; this.userModules = userModules; this.companies = companies; this.tenant = tenant;
  }

  @Transactional(readOnly = true)
  public List<FeatureModuleResponse> listForCurrentUser() {
    AppUser user = tenant.currentUser();
    Company company = tenant.currentCompany();
    Set<String> enabled = userModules.findByIdUserId(user.getId()).stream().map(UserFeatureModule::moduleCode).collect(java.util.stream.Collectors.toSet());
    return companyModuleCodes(company).stream().map(code -> response(requireModule(code), enabled.contains(code))).toList();
  }

  @Transactional(readOnly = true)
  public List<FeatureModuleResponse> listForCompany(Long companyId) {
    requireSuperAdmin();
    Company company = requireCompany(companyId);
    Set<String> enabled = companyModuleCodes(company);
    return modules.findAllByOrderByNameAsc().stream().map(module -> response(module, enabled.contains(module.getCode()))).toList();
  }

  @Transactional
  public void setCompanyEnabled(Long companyId, String code, boolean enabled) {
    requireSuperAdmin();
    Company company = requireCompany(companyId); requireModule(code);
    CompanyFeatureModuleId id = new CompanyFeatureModuleId(company.getId(), code);
    if (enabled) companyModules.save(new CompanyFeatureModule(company.getId(), code)); else companyModules.deleteById(id);
  }

  @Transactional
  public void setCurrentUserEnabled(String code, boolean enabled) {
    AppUser user = tenant.currentUser();
    Company company = tenant.currentCompany();
    requireModule(code);
    if (!companyModules.existsByIdCompanyIdAndIdModuleCode(company.getId(), code)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "模块未向当前企业开放");
    UserFeatureModuleId id = new UserFeatureModuleId(user.getId(), code);
    if (enabled) userModules.save(new UserFeatureModule(user.getId(), code)); else userModules.deleteById(id);
  }

  public void requireEnabled(String code) {
    AppUser user = tenant.currentUser();
    Company company = tenant.currentCompany();
    if (!companyModules.existsByIdCompanyIdAndIdModuleCode(company.getId(), code) || !userModules.existsByIdUserIdAndIdModuleCode(user.getId(), code)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户未启用该模块");
    }
  }

  private Set<String> companyModuleCodes(Company company) { return companyModules.findByIdCompanyId(company.getId()).stream().map(CompanyFeatureModule::moduleCode).collect(java.util.stream.Collectors.toSet()); }
  private FeatureModule requireModule(String code) { return modules.findById(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模块不存在")); }
  private Company requireCompany(Long id) { return companies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公司不存在")); }
  private void requireSuperAdmin() { if (!tenant.isSuperAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅超级管理员可配置企业模块"); }
  private FeatureModuleResponse response(FeatureModule module, boolean enabled) { return new FeatureModuleResponse(module.getCode(), module.getName(), module.getDescription(), enabled); }
}
