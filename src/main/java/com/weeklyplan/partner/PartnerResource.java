package com.weeklyplan.partner;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name = "partner_resources")
public class PartnerResource {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @Column(nullable = false) private String name; private String website; private String region;
  @Column(columnDefinition = "TEXT") private String introduction;
  @Column(name = "cooperation_status", nullable = false) private String cooperationStatus;
  @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "owner_user_id", nullable = false) private AppUser owner;
  @Column(name = "next_follow_up_date") private LocalDate nextFollowUpDate;
  private boolean preferred; @Column(name = "risk_note") private String riskNote;
  @Column(name = "technical_score") private Integer technicalScore; @Column(name = "commercial_score") private Integer commercialScore; @Column(name = "delivery_score") private Integer deliveryScore;
  @ElementCollection @CollectionTable(name="partner_resource_roles", joinColumns=@JoinColumn(name="resource_id")) @Column(name="role_code") private Set<String> roles = new LinkedHashSet<>();
  @ElementCollection @CollectionTable(name="partner_resource_tags", joinColumns=@JoinColumn(name="resource_id")) @Column(name="tag") private Set<String> tags = new LinkedHashSet<>();
  @Column(name="created_at", nullable=false) private Instant createdAt; @Column(name="updated_at", nullable=false) private Instant updatedAt;
  public Long getId(){return id;} public Company getCompany(){return company;} public String getName(){return name;} public String getWebsite(){return website;} public String getRegion(){return region;} public String getIntroduction(){return introduction;} public String getCooperationStatus(){return cooperationStatus;} public AppUser getOwner(){return owner;} public LocalDate getNextFollowUpDate(){return nextFollowUpDate;} public boolean isPreferred(){return preferred;} public String getRiskNote(){return riskNote;} public Integer getTechnicalScore(){return technicalScore;} public Integer getCommercialScore(){return commercialScore;} public Integer getDeliveryScore(){return deliveryScore;} public Set<String> getRoles(){return roles;} public Set<String> getTags(){return tags;} public Instant getCreatedAt(){return createdAt;}
  public static PartnerResource create(Company company, AppUser owner, SavePartnerResourceRequest r){ PartnerResource p=new PartnerResource(); p.company=company;p.owner=owner;p.apply(r);p.createdAt=Instant.now();return p; }
  public void apply(SavePartnerResourceRequest r){ name=r.name().trim();website=trim(r.website());region=trim(r.region());introduction=trim(r.introduction());cooperationStatus=r.cooperationStatus();nextFollowUpDate=r.nextFollowUpDate();preferred=r.preferred();riskNote=trim(r.riskNote());technicalScore=r.technicalScore();commercialScore=r.commercialScore();deliveryScore=r.deliveryScore();roles=new LinkedHashSet<>(r.roles());tags=new LinkedHashSet<>(r.tags());updatedAt=Instant.now(); }
  private String trim(String s){return s==null?null:s.trim();}
}
