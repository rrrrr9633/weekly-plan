package com.weeklyplan.ai;

import com.weeklyplan.company.Company;
import com.weeklyplan.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_operation_proposals")
public class AiOperationProposal {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false) private Company company;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requested_by_user_id", nullable = false) private AppUser requestedBy;
  @Enumerated(EnumType.STRING) @Column(name = "operation_type", nullable = false) private AiOperationType operationType;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private AiProposalStatus status;
  @Lob @Column(nullable = false) private String payload;
  @Lob private String preview;
  @Lob @Column(name = "result_json") private String resultJson;
  @Lob @Column(name = "error_message") private String errorMessage;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "confirmed_at") private Instant confirmedAt;
  @Column(name = "executed_at") private Instant executedAt;
  public Long getId() { return id; } public Company getCompany() { return company; } public AppUser getRequestedBy() { return requestedBy; }
  public AiOperationType getOperationType() { return operationType; } public AiProposalStatus getStatus() { return status; } public String getPayload() { return payload; }
  public String getPreview() { return preview; } public String getResultJson() { return resultJson; } public String getErrorMessage() { return errorMessage; }
  public Instant getCreatedAt() { return createdAt; } public Instant getConfirmedAt() { return confirmedAt; } public Instant getExecutedAt() { return executedAt; }
  public static AiOperationProposal create(Company company, AppUser user, AiOperationType type, String payload, String preview) {
    AiOperationProposal proposal = new AiOperationProposal(); proposal.company = company; proposal.requestedBy = user; proposal.operationType = type;
    proposal.status = AiProposalStatus.PENDING; proposal.payload = payload; proposal.preview = preview; proposal.createdAt = Instant.now(); return proposal;
  }
  public void confirm() { status = AiProposalStatus.EXECUTING; confirmedAt = Instant.now(); }
  public void completeReadOnly(String result) { status = AiProposalStatus.COMPLETED; resultJson = result; executedAt = Instant.now(); }
  public void complete(String result) { status = AiProposalStatus.COMPLETED; resultJson = result; executedAt = Instant.now(); }
  public void fail(String error) { status = AiProposalStatus.FAILED; errorMessage = error; executedAt = Instant.now(); }
}
