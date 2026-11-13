package com.weeklyplan.watermark;

import com.weeklyplan.company.Company;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "watermark_enterprises")
public class WatermarkEnterprise {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private String city;
  @Column(nullable = false) private String county;
  @Column(name = "visit1_at") private LocalDateTime visit1At;
  @Column(name = "visit2_at") private LocalDateTime visit2At;
  @Column(name = "visit3_at") private LocalDateTime visit3At;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public Company getCompany() { return company; }
  public String getName() { return name; }
  public String getCity() { return city; }
  public String getCounty() { return county; }
  public LocalDateTime getVisit1At() { return visit1At; }
  public LocalDateTime getVisit2At() { return visit2At; }
  public LocalDateTime getVisit3At() { return visit3At; }

  boolean sameVisits(LocalDateTime visit1, LocalDateTime visit2, LocalDateTime visit3) {
    return Objects.equals(visit1At, visit1)
        && Objects.equals(visit2At, visit2)
        && Objects.equals(visit3At, visit3);
  }

  void replaceVisits(LocalDateTime visit1, LocalDateTime visit2, LocalDateTime visit3) {
    this.visit1At = visit1;
    this.visit2At = visit2;
    this.visit3At = visit3;
    this.updatedAt = Instant.now();
  }

  public static WatermarkEnterprise create(
      Company company,
      String name,
      String city,
      String county,
      LocalDateTime visit1At,
      LocalDateTime visit2At,
      LocalDateTime visit3At
  ) {
    WatermarkEnterprise row = new WatermarkEnterprise();
    row.company = company;
    row.name = name;
    row.city = city;
    row.county = county;
    row.visit1At = visit1At;
    row.visit2At = visit2At;
    row.visit3At = visit3At;
    row.createdAt = Instant.now();
    row.updatedAt = Instant.now();
    return row;
  }
}
