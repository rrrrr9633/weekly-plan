package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weeklyplan.tenant.TenantAccessService;
import com.weeklyplan.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AiConversationService {
  private final AiConversationRepository conversations;
  private final AiConversationMessageRepository messages;
  private final AiOperationMemoryRepository memories;
  private final AiOperationProposalRepository proposals;
  private final TenantAccessService tenant;
  private final ObjectMapper mapper;

  public AiConversationService(AiConversationRepository conversations, AiConversationMessageRepository messages,
      AiOperationMemoryRepository memories, AiOperationProposalRepository proposals, TenantAccessService tenant, ObjectMapper mapper) {
    this.conversations = conversations; this.messages = messages; this.memories = memories; this.proposals = proposals; this.tenant = tenant; this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<AiDtos.ConversationSummary> listConversations() {
    AppUser user = tenant.currentUser();
    return conversations.findByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(user.getId(), Instant.now()).stream().map(this::summary).toList();
  }

  @Transactional
  public AiDtos.ConversationDetail createConversation(String title) {
    AppUser user = tenant.currentUser();
    String safeTitle = title == null || title.isBlank() ? "新对话" : title.trim();
    AiConversation conversation = conversations.save(AiConversation.create(tenant.currentCompany(), user, safeTitle));
    return detail(conversation);
  }

  @Transactional(readOnly = true)
  public AiDtos.ConversationDetail getConversation(Long id) {
    return detail(findOwned(id));
  }

  @Transactional
  public void deleteConversation(Long id) {
    conversations.delete(findOwned(id));
  }

  @Transactional(readOnly = true)
  public List<AiDtos.MemoryResponse> listMemories() {
    tenant.currentUser();
    return memories.findByCompanyIdOrderByCreatedAtDesc(tenant.currentCompany().getId()).stream().map(memory ->
      new AiDtos.MemoryResponse(memory.getId(), memory.getActor().getId(), memory.getActor().getDisplayName(),
        memory.getOperationType().name(), memory.getSummary(), memory.getCreatedAt())).toList();
  }

  @Transactional
  public void deleteOperation(Long id) {
    AppUser user = tenant.currentUser();
    AiOperationProposal proposal = proposals.findByIdAndRequestedById(id, user.getId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 操作记录不存在"));
    tenant.assertCompany(proposal.getCompany());
    proposals.delete(proposal);
  }

  public List<AiDtos.ChatMessage> historyForModel(AiConversation conversation) {
    return messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream().map(message ->
      new AiDtos.ChatMessage(message.getSender() == AiConversationMessage.Sender.USER ? "user" : "assistant", message.getContent())).toList();
  }

  public void append(AiConversation conversation, AiConversationMessage.Sender sender, String content, AiOperationProposal proposal) {
    messages.save(AiConversationMessage.create(conversation, sender, content, proposal));
    conversation.touch();
    conversations.save(conversation);
  }

  public AiConversation resolveConversation(Long id, String firstMessage) {
    if (id != null) return findOwned(id);
    return conversations.save(AiConversation.create(tenant.currentCompany(), tenant.currentUser(), titleFrom(firstMessage)));
  }

  private String titleFrom(String message) {
    String compact = message == null ? "" : message.trim().replaceAll("\\s+", " ");
    return compact.length() <= 30 ? (compact.isBlank() ? "新对话" : compact) : compact.substring(0, 30) + "…";
  }

  private AiConversation findOwned(Long id) {
    if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少会话 ID");
    AiConversation conversation = conversations.findByIdAndUserId(id, tenant.currentUser().getId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在或已过期"));
    if (conversation.getExpiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "对话已过期");
    tenant.assertCompany(conversation.getCompany());
    return conversation;
  }

  private AiDtos.ConversationSummary summary(AiConversation value) {
    return new AiDtos.ConversationSummary(value.getId(), value.getTitle(), value.getCreatedAt(), value.getUpdatedAt(), value.getExpiresAt());
  }

  private AiDtos.ConversationDetail detail(AiConversation value) {
    List<AiDtos.MessageResponse> rows = messages.findByConversationIdOrderByCreatedAtAsc(value.getId()).stream().map(message ->
      new AiDtos.MessageResponse(message.getId(), message.getSender().name(), message.getContent(), message.getProposal() == null ? null : message.getProposal().getId(), message.getCreatedAt())).toList();
    return new AiDtos.ConversationDetail(summary(value), rows);
  }
}
