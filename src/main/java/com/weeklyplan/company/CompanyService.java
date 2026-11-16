package com.weeklyplan.company;

import com.weeklyplan.project.ProjectRepository;
import com.weeklyplan.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class CompanyService {
  private final CompanyRepository companies;
  private final UserRepository users;
  private final ProjectRepository projects;

  public CompanyService(CompanyRepository companies, UserRepository users, ProjectRepository projects) {
    this.companies = companies;
    this.users = users;
    this.projects = projects;
  }

  @Transactional(readOnly = true)
  public List<CompanyResponse> listActive() {
    return companies.findAll().stream().filter(company -> "ACTIVE".equals(company.getStatus())).map(CompanyResponse::of).toList();
  }

  @Transactional(readOnly = true)
  public List<CompanyResponse> listAll() { return companies.findAll().stream().map(CompanyResponse::of).toList(); }

  @Transactional
  public CompanyResponse create(CreateCompanyRequest request) {
    String code = request.code().trim().toUpperCase(Locale.ROOT);
    if (companies.findByCode(code).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "公司编码已存在");
    return CompanyResponse.of(companies.save(Company.create(code, request.name().trim())));
  }

  @Transactional
  public void delete(Long id) {
    Company company = companies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公司不存在"));
    if (users.existsByCompanyId(id) || projects.existsByCompanyId(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "公司仍有关联用户或项目，不能删除");
    }
    companies.delete(company);
  }
}
