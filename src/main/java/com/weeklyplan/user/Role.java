package com.weeklyplan.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "roles")
public class Role {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String code;
  @Column(nullable = false) private String name;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  public Long getId() { return id; } public String getCode() { return code; }
  public String getName() { return name; }
}
