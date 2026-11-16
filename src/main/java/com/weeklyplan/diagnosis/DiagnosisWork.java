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
  @Column(name = "address", nullable = false) private String address;
  @Column(name = "county", nullable = false) private String county;
  @Column(name = "diagnosis_time", nullable = false) private String diagnosisTime;
  @Column(name = "diagnosis_round", nullable = false) private Integer diagnosisRound;
  @Column(name = "enterprise_contact") private String enterpriseContact;
  @Column(name = "enterprise_contact_phone") private String enterpriseContactPhone;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id", nullable = false) private AppUser createdBy;
  @ManyToMany @JoinTable(name = "diagnosis_work_participants", joinColumns = @JoinColumn(name = "diagnosis_work_id"), inverseJoinColumns = @JoinColumn(name = "user_id")) private Set<AppUser> participants = new LinkedHashSet<>();
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  public Long getId() { return id; } public Company getCompany() { return company; } public LocalDate getWorkDate() { return workDate; } public String getEnterpriseName() { return enterpriseName; } public String getAddress() { return address; } public String getCounty() { return county; } public String getDiagnosisTime() { return diagnosisTime; } public Integer getDiagnosisRound() { return diagnosisRound; } public String getEnterpriseContact() { return enterpriseContact; } public String getEnterpriseContactPhone() { return enterpriseContactPhone; } public AppUser getCreatedBy() { return createdBy; } public Set<AppUser> getParticipants() { return participants; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
  public static DiagnosisWork create(Company company, AppUser creator, LocalDate date, String enterpriseName, String county, String diagnosisTime, Integer diagnosisRound, String enterpriseContact, String enterpriseContactPhone, Set<AppUser> participants) { DiagnosisWork work = new DiagnosisWork(); work.company = company; work.createdBy = creator; work.workDate = date; work.enterpriseName = enterpriseName; work.address = county; work.county = county; work.diagnosisTime = diagnosisTime; work.diagnosisRound = diagnosisRound; work.enterpriseContact = enterpriseContact; work.enterpriseContactPhone = enterpriseContactPhone; work.participants = new LinkedHashSet<>(participants); work.createdAt = Instant.now(); work.updatedAt = Instant.now(); return work; }
  public void update(LocalDate date, String enterpriseName, String county, String diagnosisTime, Integer diagnosisRound, String enterpriseContact, String enterpriseContactPhone, Set<AppUser> participants) { this.workDate = date; this.enterpriseName = enterpriseName; this.address = county; this.county = county; this.diagnosisTime = diagnosisTime; this.diagnosisRound = diagnosisRound; this.enterpriseContact = enterpriseContact; this.enterpriseContactPhone = enterpriseContactPhone; this.participants = new LinkedHashSet<>(participants); this.updatedAt = Instant.now(); }
}
