package com.weeklyplan.module;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/modules")
public class FeatureModuleController {
  private final FeatureModuleService modules;
  public FeatureModuleController(FeatureModuleService modules) { this.modules = modules; }
  @GetMapping("/me") public List<FeatureModuleResponse> listMine() { return modules.listForCurrentUser(); }
  @PutMapping("/me/{code}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setMine(@PathVariable String code, @Valid @RequestBody SetFeatureModuleRequest request) { modules.setCurrentUserEnabled(code, request.enabled()); }
  @GetMapping("/companies/{companyId}") public List<FeatureModuleResponse> listCompany(@PathVariable Long companyId) { return modules.listForCompany(companyId); }
  @PutMapping("/companies/{companyId}/{code}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setCompany(@PathVariable Long companyId, @PathVariable String code, @Valid @RequestBody SetFeatureModuleRequest request) { modules.setCompanyEnabled(companyId, code, request.enabled()); }
}
