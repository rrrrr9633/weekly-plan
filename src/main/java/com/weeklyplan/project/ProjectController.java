package com.weeklyplan.project;

import com.weeklyplan.partner.PartnerResourceService;
import com.weeklyplan.partner.ProjectPartnerResourceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
  private final ProjectService projects;
  private final PartnerResourceService partners;
  public ProjectController(ProjectService projects, PartnerResourceService partners) { this.projects = projects; this.partners = partners; }

  @GetMapping public List<ProjectResponse> list() { return projects.list(); }
  @GetMapping("/{id}/partner-resources") public List<ProjectPartnerResourceResponse> partnerResources(@PathVariable Long id) { return partners.listByProject(id); }
  @GetMapping("/{id}") public ProjectResponse get(@PathVariable Long id) { return projects.get(id); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) { return projects.create(request); }
  @PutMapping("/{id}") public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) { return projects.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { projects.delete(id); }
}
