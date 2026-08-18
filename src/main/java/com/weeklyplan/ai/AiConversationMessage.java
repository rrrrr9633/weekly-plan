package com.weeklyplan.ai;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_conversation_messages")
public class AiConversationMessage {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false) private AiConversation conversation;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Sender sender;
  @Column(nullable = false) private String content;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "proposal_id") private AiOperationProposal proposal;
  @Column(name = "created_at", nullable = false) private Instant createdAt;

  public enum Sender { USER, ASSISTANT }
  public Long getId() { return id; }
  public AiConversation getConversation() { return conversation; }
  public Sender getSender() { return sender; }
  public String getContent() { return content; }
  public AiOperationProposal getProposal() { return proposal; }
  public Instant getCreatedAt() { return createdAt; }

  public static AiConversationMessage create(AiConversation conversation, Sender sender, String content, AiOperationProposal proposal) {
    AiConversationMessage value = new AiConversationMessage();
    value.conversation = conversation;
    value.sender = sender;
    value.content = content;
    value.proposal = proposal;
    value.createdAt = Instant.now();
    return value;
  }
}
