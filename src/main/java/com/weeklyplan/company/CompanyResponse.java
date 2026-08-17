package com.weeklyplan.company;

public record CompanyResponse(String id, String code, String name) {
  public static CompanyResponse of(Company company) {
    return new CompanyResponse(company.getId().toString(), company.getCode(), company.getName());
  }
}
