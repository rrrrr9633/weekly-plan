package com.weeklyplan.auth;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registration_requests")
public class RegistrationRequest {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id") private Company company;
  @Column(name = "new_company_name") private String newCompanyName;
  @Column(name = "new_company_code") private String newCompanyCode;
  @Column(nullable = false) private String status;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_user_id") private AppUser reviewedBy;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "reviewed_at") private Instant reviewedAt;

  public Long getId() { return id; }
  public AppUser getUser() { return user; }
  public Company getCompany() { return company; }
  public String getNewCompanyName() { return newCompanyName; }
  public String getNewCompanyCode() { return newCompanyCode; }
  public String getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }
  public static RegistrationRequest existing(AppUser user, Company company) { return create(user, company, null, null); }
  public static RegistrationRequest newCompany(AppUser user, String name, String code) { return create(user, null, name, code); }
  private static RegistrationRequest create(AppUser user, Company company, String name, String code) {
    RegistrationRequest request = new RegistrationRequest(); request.user = user; request.company = company; request.newCompanyName = name; request.newCompanyCode = code; request.status = "PENDING"; request.createdAt = Instant.now(); return request;
  }
  public void approve(AppUser reviewer) { status = "APPROVED"; reviewedBy = reviewer; reviewedAt = Instant.now(); }
  public void reject(AppUser reviewer) { status = "REJECTED"; reviewedBy = reviewer; reviewedAt = Instant.now(); }
}
