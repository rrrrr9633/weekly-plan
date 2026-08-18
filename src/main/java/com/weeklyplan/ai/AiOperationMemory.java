package com.weeklyplan.ai;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_operation_memories")
public class AiOperationMemory {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id", nullable = false) private AppUser actor;
  @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "proposal_id", nullable = false, unique = true) private AiOperationProposal proposal;
  @Enumerated(EnumType.STRING) @Column(name = "operation_type", nullable = false) private AiOperationType operationType;
  @Column(nullable = false) private String summary;
  @Column(name = "created_at", nullable = false) private Instant createdAt;

  public Long getId() { return id; }
  public Company getCompany() { return company; }
  public AppUser getActor() { return actor; }
  public AiOperationProposal getProposal() { return proposal; }
  public AiOperationType getOperationType() { return operationType; }
  public String getSummary() { return summary; }
  public Instant getCreatedAt() { return createdAt; }

  public static AiOperationMemory create(AiOperationProposal proposal, String summary) {
    AiOperationMemory value = new AiOperationMemory();
    value.company = proposal.getCompany();
    value.actor = proposal.getRequestedBy();
    value.proposal = proposal;
    value.operationType = proposal.getOperationType();
    value.summary = summary;
    value.createdAt = Instant.now();
    return value;
  }
}
