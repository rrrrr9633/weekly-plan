package com.weeklyplan.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/registration-requests")
public class RegistrationApprovalController {
  private final RegistrationApprovalService approvals;
  public RegistrationApprovalController(RegistrationApprovalService approvals) { this.approvals = approvals; }
  @GetMapping("/pending") public List<RegistrationRequestResponse> pending() { return approvals.pending(); }
  @PutMapping("/{id}") public RegistrationRequestResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRegistrationRequest command) { return approvals.review(id, command); }
}
