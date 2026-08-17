package com.weeklyplan.user;

import com.weeklyplan.company.Company;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class AppUser {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "user_code", nullable = false, unique = true) private String userCode;
  @Column(nullable = false, unique = true) private String username;
  @Column(name = "password_hash", nullable = false) private String passwordHash;
  @Column(name = "display_name", nullable = false) private String displayName;
  private String email;
  private String phone;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private UserStatus status;
  @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "role_id", nullable = false) private Role role;
  @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "company_id") private Company company;
  @Column(name = "last_login_at") private Instant lastLoginAt;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  public Long getId() { return id; } public String getUserCode() { return userCode; } public String getUsername() { return username; }
  public String getDisplayName() { return displayName; } public Role getRole() { return role; } public Company getCompany() { return company; } public UserStatus getStatus() { return status; }
  public String getPasswordHash() { return passwordHash; } public Instant getCreatedAt() { return createdAt; }
  public void setCompany(Company company) { this.company = company; }
  public void setLastLoginAt(Instant value) { lastLoginAt = value; }
  public void updatePassword(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = Instant.now(); }
  public void update(String username, String displayName, Role role) {
    this.username = username; this.displayName = displayName; this.role = role; this.updatedAt = Instant.now();
  }
  public static AppUser create(String userCode, String username, String passwordHash, String displayName, Role role) {
    AppUser user = new AppUser();
    user.userCode = userCode; user.username = username; user.passwordHash = passwordHash; user.displayName = displayName;
    user.role = role; user.status = UserStatus.ACTIVE; user.createdAt = Instant.now(); user.updatedAt = Instant.now();
    return user;
  }
}
