package com.weeklyplan.diagnosis;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "diagnosis_works")
public class DiagnosisWork {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @Column(name = "work_date", nullable = false) private LocalDate workDate;
  @Column(name = "enterprise_name", nullable = false) private String enterpriseName;
  @Column(nullable = false) private String address;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id", nullable = false) private AppUser createdBy;
  @ManyToMany @JoinTable(name = "diagnosis_work_participants", joinColumns = @JoinColumn(name = "diagnosis_work_id"), inverseJoinColumns = @JoinColumn(name = "user_id")) private Set<AppUser> participants = new LinkedHashSet<>();
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  public Long getId() { return id; } public Company getCompany() { return company; } public LocalDate getWorkDate() { return workDate; } public String getEnterpriseName() { return enterpriseName; } public String getAddress() { return address; } public AppUser getCreatedBy() { return createdBy; } public Set<AppUser> getParticipants() { return participants; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
  public static DiagnosisWork create(Company company, AppUser creator, LocalDate date, String enterpriseName, String address, Set<AppUser> participants) { DiagnosisWork work = new DiagnosisWork(); work.company = company; work.createdBy = creator; work.workDate = date; work.enterpriseName = enterpriseName; work.address = address; work.participants = new LinkedHashSet<>(participants); work.createdAt = Instant.now(); work.updatedAt = Instant.now(); return work; }
  public void update(LocalDate date, String enterpriseName, String address, Set<AppUser> participants) { this.workDate = date; this.enterpriseName = enterpriseName; this.address = address; this.participants = new LinkedHashSet<>(participants); this.updatedAt = Instant.now(); }
}
