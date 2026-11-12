package com.weeklyplan.watermark;

import com.weeklyplan.company.Company;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "watermark_enterprises")
public class WatermarkEnterprise {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private String city;
  @Column(nullable = false) private String county;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public Company getCompany() { return company; }
  public String getName() { return name; }
  public String getCity() { return city; }
  public String getCounty() { return county; }

  public static WatermarkEnterprise create(Company company, String name, String city, String county) {
    WatermarkEnterprise row = new WatermarkEnterprise();
    row.company = company;
    row.name = name;
    row.city = city;
    row.county = county;
    row.createdAt = Instant.now();
    row.updatedAt = Instant.now();
    return row;
  }
}
