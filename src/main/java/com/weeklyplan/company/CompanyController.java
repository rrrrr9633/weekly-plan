package com.weeklyplan.company;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/companies")
public class CompanyController {
  private final CompanyService companies;
  public CompanyController(CompanyService companies) { this.companies = companies; }

  @GetMapping
  public List<CompanyResponse> listAll() { return companies.listAll(); }

  @GetMapping("/public")
  public List<CompanyResponse> listActive() { return companies.listActive(); }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) { return companies.create(request); }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) { companies.delete(id); }
}
