package com.weeklyplan.company;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "companies")
public class Company {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String code;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private String status;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
  public String getStatus() { return status; }
  public static Company create(String code, String name) {
    Company company = new Company();
    company.code = code;
    company.name = name;
    company.status = "ACTIVE";
    company.createdAt = Instant.now();
    company.updatedAt = Instant.now();
    return company;
  }
}
