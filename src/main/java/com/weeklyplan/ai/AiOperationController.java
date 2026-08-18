package com.weeklyplan.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiOperationController {
  private final AiOperationService service;
  public AiOperationController(AiOperationService service) { this.service = service; }
  @PostMapping("/proposals") public AiDtos.ProposalResponse propose(@Valid @RequestBody AiDtos.ConversationRequest request) { return service.propose(request.message()); }
  @GetMapping("/context") public Map<String, Object> context() { return service.context(); }
  @PostMapping("/proposals/{id}/confirm") public AiDtos.ProposalResponse confirm(@PathVariable Long id) { return service.confirm(id); }
  @PostMapping("/proposals/{id}/supplement") public AiDtos.ProposalResponse supplement(@PathVariable Long id, @RequestBody AiDtos.SupplementRequest request) { return service.supplement(id, request); }
  @GetMapping("/operations") public List<AiDtos.ProposalResponse> history() { return service.history(); }
}
