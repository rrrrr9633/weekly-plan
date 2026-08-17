package com.weeklyplan.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/plans")
public class WeekPlanController {
  private final WeekPlanService plans;
  private final WeekPlanReportService reports;
  public WeekPlanController(WeekPlanService plans, WeekPlanReportService reports) {
    this.plans = plans;
    this.reports = reports;
  }

  @GetMapping("/my/{year}/{week}")
  public List<WeekPlanResponse> listMine(Authentication authentication, @PathVariable int year, @PathVariable int week) {
    return plans.listMine(authentication.getName(), year, week);
  }

  @GetMapping("/my/{year}/{week}/report")
  public ResponseEntity<byte[]> exportMyReport(Authentication authentication, @PathVariable int year, @PathVariable int week) {
    byte[] report = reports.exportMine(Long.valueOf(authentication.getName()), year, week);
    String filename = year + "年第" + week + "周个人周报.xlsx";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
        .body(report);
  }

  @GetMapping("/week/{year}/{week}")
  public List<WeekPlanResponse> listTeam(@PathVariable int year, @PathVariable int week) { return plans.listTeam(year, week); }

  @GetMapping("/archived")
  public List<WeekPlanResponse> listArchived(Authentication authentication) {
    return plans.listArchived(authentication.getName());
  }

  @PutMapping("/board/order")
  public List<WeekPlanResponse> saveBoardOrder(Authentication authentication, @Valid @RequestBody SaveBoardOrderRequest request) {
    return plans.saveBoardOrder(authentication.getName(), request);
  }

  @DeleteMapping("/board/order") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void restoreBoardOrder(
      Authentication authentication,
      @RequestParam Long projectId,
      @RequestParam @Min(2000) @Max(2100) int year,
      @RequestParam @Min(1) @Max(53) int weekNumber,
      @RequestParam PlanWeekday weekday
  ) {
    plans.restoreBoardOrder(authentication.getName(), projectId, year, weekNumber, weekday);
  }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public WeekPlanResponse create(Authentication authentication, @Valid @RequestBody CreateWeekPlanRequest request) {
    return plans.create(authentication.getName(), request);
  }

  @PostMapping("/batch") @ResponseStatus(HttpStatus.CREATED)
  public List<WeekPlanResponse> createBatch(Authentication authentication, @Valid @RequestBody BatchCreateWeekPlanRequest request) {
    return plans.createBatch(authentication.getName(), request);
  }

  @PostMapping("/{id}/claim")
  public WeekPlanResponse claim(Authentication authentication, @PathVariable Long id) {
    return plans.claim(authentication.getName(), id);
  }

  @DeleteMapping("/{id}/claim") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(Authentication authentication, @PathVariable Long id) {
    plans.leave(authentication.getName(), id);
  }

  @PutMapping("/{id}")
  public WeekPlanResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateWeekPlanRequest request) {
    return plans.update(authentication.getName(), id, request);
  }

  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication authentication, @PathVariable Long id) { plans.delete(authentication.getName(), id); }

  @PutMapping("/{id}/archive")
  public WeekPlanResponse archive(Authentication authentication, @PathVariable Long id) {
    return plans.archive(authentication.getName(), id);
  }

  @PutMapping("/{id}/restore")
  public WeekPlanResponse restore(Authentication authentication, @PathVariable Long id) {
    return plans.restore(authentication.getName(), id);
  }

  @PostMapping("/assign") @ResponseStatus(HttpStatus.CREATED)
  public WeekPlanResponse assign(Authentication authentication, @Valid @RequestBody AssignWeekPlanRequest request) {
    return plans.assign(authentication.getName(), request);
  }
}
