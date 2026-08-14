package com.weeklyplan.project;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
  private final ProjectService projects;
  public ProjectController(ProjectService projects) { this.projects = projects; }

  @GetMapping public List<ProjectResponse> list() { return projects.list(); }
  @GetMapping("/{id}") public ProjectResponse get(@PathVariable Long id) { return projects.get(id); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) { return projects.create(request); }
  @PutMapping("/{id}") public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) { return projects.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { projects.delete(id); }
}
