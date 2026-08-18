package com.weeklyplan.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiOperationController {
  private final AiOperationService service;
  private final AiConversationService conversations;
  public AiOperationController(AiOperationService service, AiConversationService conversations) { this.service = service; this.conversations = conversations; }
  @PostMapping("/proposals") public AiDtos.ProposalResponse propose(@Valid @RequestBody AiDtos.ConversationRequest request) { return service.propose(request); }
  @GetMapping("/context") public Map<String, Object> context() { return service.context(); }
  @PostMapping("/proposals/{id}/confirm") public AiDtos.ProposalResponse confirm(@PathVariable Long id) { return service.confirm(id); }
  @PostMapping("/proposals/{id}/supplement") public AiDtos.ProposalResponse supplement(@PathVariable Long id, @RequestBody AiDtos.SupplementRequest request) { return service.supplement(id, request); }
  @GetMapping("/operations") public List<AiDtos.ProposalResponse> history() { return service.history(); }
  @DeleteMapping("/operations/{id}") public void deleteOperation(@PathVariable Long id) { conversations.deleteOperation(id); }
  @GetMapping("/memories") public List<AiDtos.MemoryResponse> memories() { return conversations.listMemories(); }
  @GetMapping("/conversations") public List<AiDtos.ConversationSummary> conversations() { return conversations.listConversations(); }
  @PostMapping("/conversations") public AiDtos.ConversationDetail createConversation(@RequestBody(required = false) AiDtos.ConversationCreateRequest request) { return conversations.createConversation(request == null ? null : request.title()); }
  @GetMapping("/conversations/{id}") public AiDtos.ConversationDetail conversation(@PathVariable Long id) { return conversations.getConversation(id); }
  @DeleteMapping("/conversations/{id}") public void deleteConversation(@PathVariable Long id) { conversations.deleteConversation(id); }
}
