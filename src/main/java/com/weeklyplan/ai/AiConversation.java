package com.weeklyplan.ai;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_conversations")
public class AiConversation {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
  @Column(nullable = false) private String title;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  public Long getId() { return id; } public AppUser getUser() { return user; } public Instant getExpiresAt() { return expiresAt; }
  public static AiConversation create(Company company, AppUser user, String title) {
    AiConversation value = new AiConversation(); Instant now = Instant.now();
    value.company = company; value.user = user; value.title = title; value.createdAt = now; value.updatedAt = now; value.expiresAt = now.plus(java.time.Duration.ofDays(3)); return value;
  }
  public void touch() { updatedAt = Instant.now(); }
}
