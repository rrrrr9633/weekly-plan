package com.weeklyplan.diagnosis;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/diagnosis-works")
public class DiagnosisWorkController {
  private final DiagnosisWorkService works;
  public DiagnosisWorkController(DiagnosisWorkService works) { this.works = works; }
  @GetMapping public List<DiagnosisWorkResponse> list(@RequestParam LocalDate start, @RequestParam LocalDate end) { return works.list(start, end); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public DiagnosisWorkResponse create(@Valid @RequestBody SaveDiagnosisWorkRequest request) { return works.create(request); }
  @PutMapping("/{id}") public DiagnosisWorkResponse update(@PathVariable Long id, @Valid @RequestBody SaveDiagnosisWorkRequest request) { return works.update(id, request); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { works.delete(id); }
}
