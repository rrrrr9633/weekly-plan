package com.weeklyplan.project;

import com.weeklyplan.company.Company;
import com.weeklyplan.tenant.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final TenantAccessService tenant;

  public ProjectService(ProjectRepository projects, TenantAccessService tenant) {
    this.projects = projects;
    this.tenant = tenant;
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> list() {
    return projects.findByCompanyId(tenant.currentCompany().getId()).stream().map(ProjectResponse::of).toList();
  }

  @Transactional(readOnly = true)
  public ProjectResponse get(Long id) { return ProjectResponse.of(requireProject(id)); }

  @Transactional
  public ProjectResponse create(CreateProjectRequest request) {
    Company company = tenant.currentCompany();
    String code = request.code().trim().toUpperCase(Locale.ROOT);
    if (projects.existsByCompanyIdAndCode(company.getId(), code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "项目编码已存在");
    }
    return ProjectResponse.of(projects.save(Project.create(company, request.name().trim(), code, trimOrNull(request.description()), trimOrNull(request.assistOrg()))));
  }

  @Transactional
  public ProjectResponse update(Long id, UpdateProjectRequest request) {
    ProjectStatus status = request.status() == null ? null : parseStatus(request.status());
    Project project = requireProject(id);
    project.update(trimOrNull(request.name()), trimOrNull(request.description()), trimOrNull(request.assistOrg()), status);
    return ProjectResponse.of(project);
  }

  @Transactional
  public void delete(Long id) { projects.delete(requireProject(id)); }

  public Project requireProject(Long id) {
    Project project = projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在"));
    tenant.assertCompany(project.getCompany());
    return project;
  }

  private ProjectStatus parseStatus(String value) {
    try { return ProjectStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
    catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目状态仅支持 active 或 inactive"); }
  }

  private String trimOrNull(String value) { return value == null ? null : value.trim(); }
}
