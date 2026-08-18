package com.weeklyplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.weeklyplan.plan.*;
import com.weeklyplan.project.*;
import com.weeklyplan.tenant.TenantAccessService;
import com.weeklyplan.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class AiOperationService {
  private final AiOperationProposalRepository proposals; private final OpenAiResponsesClient client; private final ObjectMapper mapper;
  private final TenantAccessService tenant; private final ProjectRepository projects; private final WeekPlanRepository planRepository; private final WeekPlanService plans; private final ProjectService projectService;
  public AiOperationService(AiOperationProposalRepository proposals, OpenAiResponsesClient client, ObjectMapper mapper, TenantAccessService tenant, ProjectRepository projects, WeekPlanRepository planRepository, WeekPlanService plans, ProjectService projectService) {
    this.proposals = proposals; this.client = client; this.mapper = mapper; this.tenant = tenant; this.projects = projects; this.planRepository = planRepository; this.plans = plans; this.projectService = projectService;
  }
  @Transactional
  public AiDtos.ProposalResponse propose(String message) {
    AppUser user = tenant.currentUser(); List<Map<String, Object>> candidates = projectCandidates();
    LocalDate today = LocalDate.now(); WeekFields iso = WeekFields.ISO;
    AiDtos.ModelProposal model = client.propose(message, candidates, chatProjectContext(), chatPlanContext(), today.get(iso.weekBasedYear()), today.get(iso.weekOfWeekBasedYear()));
    AiOperationType type = parseType(model.operationType()); JsonNode payload = model.payload();
    // 只读诉求统一由模型根据受控上下文回答，禁止返回服务端的机械查询统计。
    if (type == AiOperationType.QUERY) { type = AiOperationType.CHAT; payload = mapper.createObjectNode(); }
    if (payload == null || !payload.isObject() || containsForbiddenField(payload)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 提案包含不允许的身份或租户字段");
    ObjectNode completed = ((ObjectNode) payload).deepCopy();
    if (type == AiOperationType.PLAN_CREATE) { normalizePlanCreatePayload(completed); applyScheduleDefaults(completed, message, today); }
    Object missing = completePayload(type, completed, user);
    AiOperationProposal proposal = proposals.save(AiOperationProposal.create(tenant.currentCompany(), user, type, json(completed), preview(type, completed, model.preview())));
    if (type == AiOperationType.QUERY) proposal.completeReadOnly(json(missing == null ? query(completed) : Map.of("missingFields", missing, "readOnly", true)));
    if (type == AiOperationType.CHAT) proposal.completeReadOnly(json(Map.of("answer", proposal.getPreview())));
    return response(proposal, missing);
  }
  @Transactional(readOnly = true)
  public Map<String, Object> context() { return Map.of("projects", projectCandidates()); }
  @Transactional(readOnly = true)
  public List<AiDtos.ProposalResponse> history() { AppUser user = tenant.currentUser(); return proposals.findByRequestedByIdOrderByCreatedAtDesc(user.getId()).stream().map(p -> response(p, null)).toList(); }
  @Transactional
  public AiDtos.ProposalResponse confirm(Long id) {
    AppUser user = tenant.currentUser(); AiOperationProposal proposal = proposals.findByIdAndRequestedById(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 提案不存在")); tenant.assertCompany(proposal.getCompany());
    if (proposal.getStatus() == AiProposalStatus.COMPLETED || proposal.getStatus() != AiProposalStatus.PENDING) return response(proposal, null);
    ObjectNode payload = (ObjectNode) tree(proposal.getPayload()); Object missing = completePayload(proposal.getOperationType(), payload, user);
    if (missing != null) return response(proposal, missing);
    proposal.confirm(); try { proposal.complete(json(execute(proposal.getOperationType(), payload))); } catch (ResponseStatusException error) { proposal.fail(error.getReason()); throw error; }
    return response(proposal, null);
  }
  @Transactional
  public AiDtos.ProposalResponse supplement(Long id, AiDtos.SupplementRequest request) {
    AppUser user = tenant.currentUser(); AiOperationProposal proposal = proposals.findByIdAndRequestedById(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 提案不存在")); tenant.assertCompany(proposal.getCompany());
    if (proposal.getStatus() != AiProposalStatus.PENDING) return response(proposal, null);
    ObjectNode payload = (ObjectNode) tree(proposal.getPayload());
    Map<String, String> fields = request.fields() == null ? Map.of() : request.fields();
    for (String name : editableFields(proposal.getOperationType())) {
      String value = fields.get(name);
      if (value != null && !value.isBlank()) payload.put(name, value.trim());
    }
    Object missing = completePayload(proposal.getOperationType(), payload, user);
    proposal.updateDraft(json(payload), preview(proposal.getOperationType(), payload, proposal.getPreview()));
    return response(proposal, missing);
  }
  private Set<String> editableFields(AiOperationType type) {
    return switch (type) {
      case PLAN_CREATE -> Set.of("projectId", "content", "weekday");
      case PLAN_UPDATE, PLAN_DELETE, PROJECT_UPDATE, PROJECT_DELETE -> Set.of("id");
      case PROJECT_CREATE -> Set.of("name", "code", "assistOrg", "description");
      case QUERY -> Set.of("resource");
      case CHAT -> Set.of();
    };
  }
  private Object completePayload(AiOperationType type, ObjectNode p, AppUser user) {
    if (type == AiOperationType.PLAN_CREATE) {
      WeekFields iso = WeekFields.ISO;
      LocalDate today = LocalDate.now();
      // 周数是系统上下文，不是用户补充字段；旧提案缺失时也统一落到当前周。
      fill(p, "year", today.get(iso.weekBasedYear()));
      fill(p, "weekNumber", today.get(iso.weekOfWeekBasedYear()));
    }
    if (type == AiOperationType.PLAN_UPDATE && p.hasNonNull("id")) { WeekPlan plan = ownedPlan(p.path("id").asLong(), user); fill(p, "projectId", plan.getProject().getId()); fill(p, "content", plan.getContent()); fill(p, "weekday", plan.getWeekday().name()); }
    if (type == AiOperationType.PROJECT_UPDATE && p.hasNonNull("id")) { Project project = currentProject(p.path("id").asLong()); fill(p, "name", project.getName()); fill(p, "description", project.getDescription()); fill(p, "assistOrg", project.getAssistOrg()); fill(p, "status", project.getStatus().name()); fill(p, "hidden", project.isHidden()); }
    ArrayList<String> fields = new ArrayList<>();
    if (type == AiOperationType.PLAN_CREATE) { required(p, fields, "projectId"); required(p, fields, "year"); required(p, fields, "weekNumber"); required(p, fields, "weekday"); required(p, fields, "content"); }
    if (type == AiOperationType.PLAN_UPDATE || type == AiOperationType.PROJECT_UPDATE || type == AiOperationType.PLAN_DELETE || type == AiOperationType.PROJECT_DELETE) required(p, fields, "id");
    if (type == AiOperationType.PROJECT_CREATE) { required(p, fields, "name"); required(p, fields, "code"); required(p, fields, "assistOrg"); }
    if (type == AiOperationType.QUERY && !p.hasNonNull("resource")) fields.add("resource (plans 或 projects)");
    return fields.isEmpty() ? null : Map.of("missingFields", fields, "projects", projectCandidates(), "targets", targetHints(type, user));
  }
  private Object execute(AiOperationType type, JsonNode p) {
    return switch (type) {
      case PLAN_CREATE -> plans.aiCreate(new CreateWeekPlanRequest(p.path("projectId").asLong(), p.path("year").asInt(), p.path("weekNumber").asInt(), weekday(p), p.path("content").asText()));
      case PLAN_UPDATE -> plans.aiUpdateOwned(requiredId(p), new UpdateWeekPlanRequest(p.path("projectId").asLong(), p.path("content").asText(), weekday(p)));
      case PLAN_DELETE -> { plans.aiDeleteOwned(requiredId(p)); yield Map.of("deleted", true); }
      case PROJECT_CREATE -> projectService.aiCreate(new CreateProjectRequest(p.path("name").asText(), p.path("code").asText(), nullable(p,"description"), nullable(p,"assistOrg")));
      case PROJECT_UPDATE -> projectService.aiUpdate(requiredId(p), new UpdateProjectRequest(nullable(p,"name"), nullable(p,"description"), nullable(p,"assistOrg"), nullable(p,"status"), p.has("hidden") ? p.get("hidden").asBoolean() : null));
      case PROJECT_DELETE -> { projectService.aiDelete(requiredId(p)); yield Map.of("deleted", true); }
      case QUERY -> query(p);
      case CHAT -> Map.of("answer", p.path("answer").asText(""));
    };
  }
  private Object query(JsonNode p) {
    String resource = p.path("resource").asText().toLowerCase(Locale.ROOT); if (!resource.equals("plans") && !resource.equals("projects")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUERY 仅支持 plans 或 projects");
    if (resource.equals("projects")) { String keyword = p.path("keyword").asText("").toLowerCase(Locale.ROOT); return projects.findByCompanyId(tenant.currentCompany().getId()).stream().filter(x -> keyword.isBlank() || x.getName().toLowerCase().contains(keyword) || x.getCode().toLowerCase().contains(keyword)).map(x -> Map.of("id", x.getId(), "code", x.getCode(), "name", x.getName(), "assistOrg", x.getAssistOrg() == null ? "" : x.getAssistOrg(), "status", x.getStatus().name())).toList(); }
    return planRepository.findAll().stream().filter(x -> x.getProject().getCompany().getId().equals(tenant.currentCompany().getId())).filter(x -> !p.has("year") || x.getYear() == p.path("year").asInt()).filter(x -> !p.has("weekNumber") || x.getWeekNumber() == p.path("weekNumber").asInt()).filter(x -> !p.has("weekday") || x.getWeekday().name().equalsIgnoreCase(p.path("weekday").asText())).map(x -> Map.of("id", x.getId(), "year", x.getYear(), "weekNumber", x.getWeekNumber(), "weekday", x.getWeekday().name(), "content", x.getContent(), "project", x.getProject().getName(), "projectCode", x.getProject().getCode(), "owner", x.getUser().getDisplayName())).toList();
  }
  private WeekPlan ownedPlan(long id, AppUser user) { WeekPlan plan = planRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在")); tenant.assertCompany(plan.getProject().getCompany()); if (!plan.getUser().getId().equals(user.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在或无权操作"); return plan; }
  private Project currentProject(long id) { Project project = projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在")); tenant.assertCompany(project.getCompany()); return project; }
  private List<Map<String, Object>> projectCandidates() { return projects.findByCompanyId(tenant.currentCompany().getId()).stream().map(x -> Map.of("id", (Object)x.getId(), "code", (Object)x.getCode(), "name", (Object)x.getName())).toList(); }
  private List<Map<String, Object>> chatProjectContext() {
    return (tenant.isSuperAdmin() ? projects.findAll() : projects.findByCompanyId(tenant.currentCompany().getId())).stream()
      .map(project -> Map.of("code", (Object) project.getCode(), "name", (Object) project.getName(), "assistOrg", (Object) (project.getAssistOrg() == null ? "" : project.getAssistOrg()), "status", (Object) project.getStatus().name()))
      .toList();
  }
  private List<Map<String, Object>> chatPlanContext() {
    List<WeekPlan> readablePlans = tenant.isSuperAdmin() ? planRepository.findAll() : planRepository.findByProjectCompanyId(tenant.currentCompany().getId());
    return readablePlans.stream().map(plan -> Map.of(
      "year", (Object) plan.getYear(), "weekNumber", (Object) plan.getWeekNumber(), "weekday", (Object) weekdayLabel(plan.getWeekday().name()),
      "content", (Object) plan.getContent(), "project", (Object) plan.getProject().getName(), "owner", (Object) plan.getUser().getDisplayName(), "status", (Object) plan.getStatus().name()
    )).toList();
  }
  private Object targetHints(AiOperationType type, AppUser user) { if (type != AiOperationType.PLAN_UPDATE && type != AiOperationType.PLAN_DELETE) return List.of(); return planRepository.findAll().stream().filter(x -> x.getProject().getCompany().getId().equals(tenant.currentCompany().getId()) && x.getUser().getId().equals(user.getId())).map(x -> Map.of("id",x.getId(),"content",x.getContent(),"project",x.getProject().getName(),"weekday",x.getWeekday().name())).toList(); }
  private String preview(AiOperationType type, JsonNode p, String modelPreview) {
    if (type == AiOperationType.PLAN_CREATE) {
      String project = projects.findById(p.path("projectId").asLong()).map(Project::getName).orElse("未指定项目");
      return "将创建计划：" + p.path("content").asText("未填写内容") + "；项目：" + project + "；ISO " + p.path("year").asInt() + " 年第 " + p.path("weekNumber").asInt() + " 周，" + weekdayLabel(p.path("weekday").asText());
    }
    return modelPreview == null || modelPreview.isBlank() ? "已生成操作预览" : modelPreview;
  }
  /** Accept common model aliases so a valid creation request cannot get stuck at confirmation. */
  private void normalizePlanCreatePayload(ObjectNode p) {
    if (!p.hasNonNull("content")) {
      for (String alias : List.of("name", "title", "task")) {
        if (p.hasNonNull(alias) && !p.path(alias).asText().isBlank()) { p.set("content", p.get(alias)); break; }
      }
    }
    if (p.path("weekday").asText().equalsIgnoreCase("PENDING")) p.remove("weekday");
    if (p.path("weekday").canConvertToInt() && !p.path("weekday").isTextual()) {
      int day = p.path("weekday").asInt();
      if (day >= 1 && day <= 7) p.put("weekday", PlanWeekday.values()[day - 1].name());
    }
  }
  /** The date is user input, so it takes precedence over a model guess. */
  private void applyScheduleDefaults(ObjectNode p, String message, LocalDate today) {
    LocalDate date = resolveRequestedDate(message, today);
    WeekFields iso = WeekFields.ISO;
    String weekday = weekdayName(message, date);
    if (hasScheduleHint(message)) {
      p.put("year", date.get(iso.weekBasedYear()));
      p.put("weekNumber", date.get(iso.weekOfWeekBasedYear()));
      p.put("weekday", weekday);
    } else {
      // 未指定日期时，周数始终由系统固定为当前周，模型不得猜测或延续其他周。
      p.put("year", today.get(iso.weekBasedYear()));
      p.put("weekNumber", today.get(iso.weekOfWeekBasedYear()));
      if (!weekday.equals("PENDING")) p.put("weekday", weekday);
    }
  }
  private LocalDate resolveRequestedDate(String message, LocalDate today) {
    String text = message.replaceAll("\\s+", "");
    java.util.regex.Matcher full = java.util.regex.Pattern.compile("(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?").matcher(text);
    if (full.find()) return dateOrDefault(full.group(1), full.group(2), full.group(3), today);
    java.util.regex.Matcher monthDay = java.util.regex.Pattern.compile("(\\d{1,2})月(\\d{1,2})日?").matcher(text);
    if (monthDay.find()) return dateOrDefault(String.valueOf(today.getYear()), monthDay.group(1), monthDay.group(2), today);
    if (text.contains("后天")) return today.plusDays(2);
    if (text.contains("明天")) return today.plusDays(1);
    if (text.contains("下周") || text.contains("下星期")) return today.plusWeeks(1);
    return today;
  }
  private LocalDate dateOrDefault(String year, String month, String day, LocalDate fallback) {
    try { return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day)); }
    catch (RuntimeException ignored) { return fallback; }
  }
  private String weekdayName(String message, LocalDate date) {
    String text = message.replaceAll("\\s+", "");
    java.util.regex.Matcher weekday = java.util.regex.Pattern.compile("(?:周|星期|礼拜)([一二三四五六日天])").matcher(text);
    if (!weekday.find()) return hasExactDay(message) ? PlanWeekday.values()[date.getDayOfWeek().getValue() - 1].name() : "PENDING";
    return switch (weekday.group(1)) { case "一" -> "MONDAY"; case "二" -> "TUESDAY"; case "三" -> "WEDNESDAY"; case "四" -> "THURSDAY"; case "五" -> "FRIDAY"; case "六" -> "SATURDAY"; default -> "SUNDAY"; };
  }
  private boolean hasScheduleHint(String message) { return hasExactDay(message) || message.contains("下周") || message.contains("下星期") || message.contains("本周") || message.contains("本星期") || java.util.regex.Pattern.compile("(?:周|星期|礼拜)[一二三四五六日天]").matcher(message).find(); }
  private boolean hasExactDay(String message) { return message.contains("明天") || message.contains("后天") || java.util.regex.Pattern.compile("20\\d{2}[-/.年]\\d{1,2}[-/.月]\\d{1,2}日?|\\d{1,2}月\\d{1,2}日?").matcher(message).find(); }
  private String weekdayLabel(String weekday) {
    return switch (weekday) { case "MONDAY" -> "周一"; case "TUESDAY" -> "周二"; case "WEDNESDAY" -> "周三"; case "THURSDAY" -> "周四"; case "FRIDAY" -> "周五"; case "SATURDAY" -> "周六"; case "SUNDAY" -> "周日"; default -> "待安排"; };
  }
  private void fill(ObjectNode p, String field, Object value) { if (!p.hasNonNull(field) && value != null) p.set(field, mapper.valueToTree(value)); }
  private void required(JsonNode p, List<String> fields, String field) { if (!p.hasNonNull(field) || p.path(field).asText().isBlank()) fields.add(field); }
  private boolean containsForbiddenField(JsonNode node) { if (node.isObject()) { var it=node.fieldNames(); while(it.hasNext()) { String k=it.next().toLowerCase(); if(k.contains("user")||k.contains("company")||k.contains("role")||k.contains("auth")||k.contains("member")) return true; } } for(JsonNode c:node) if(containsForbiddenField(c)) return true; return false; }
  private AiOperationType parseType(String v) { try{return AiOperationType.valueOf(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"AI 返回了不支持的操作类型");} }
  private Long requiredId(JsonNode p) { if(!p.hasNonNull("id")||p.path("id").asLong()<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"缺少资源 id"); return p.path("id").asLong(); }
  private PlanWeekday weekday(JsonNode p) { try{return PlanWeekday.valueOf(p.path("weekday").asText().toUpperCase());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"weekday 无效");} }
  private String nullable(JsonNode p,String f){return p.hasNonNull(f)?p.path(f).asText():null;} private String json(Object v){try{return mapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException(e);}} private JsonNode tree(String v){try{return mapper.readTree(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"AI 提案数据无效");}}
  private AiDtos.ProposalResponse response(AiOperationProposal p, Object missing) {
    return response(p, p.getOperationType(), null, p.getPreview(), missing, p.getErrorMessage());
  }
  private AiDtos.ProposalResponse response(AiOperationProposal p, AiOperationType type, JsonNode payload, String preview, Object missing, String error) {
    String message;
    if (missing != null) message = "请补充以下信息，然后生成可确认的操作。";
    else if (p != null && p.getStatus() == AiProposalStatus.PENDING) message = "已生成预览；确认后才会写入。";
    else if (p != null && p.getStatus() == AiProposalStatus.COMPLETED && p.getOperationType() == AiOperationType.QUERY) message = querySummary(p.getResultJson());
    else if (p != null && p.getStatus() == AiProposalStatus.COMPLETED && p.getOperationType() == AiOperationType.CHAT) message = chatAnswer(p.getResultJson(), p.getPreview());
    else if (p != null && p.getStatus() == AiProposalStatus.COMPLETED) message = "操作已完成。";
    else message = error;
    return new AiDtos.ProposalResponse(p == null ? null : p.getId(), type.name(), p == null ? "NEEDS_INPUT" : p.getStatus().name(), preview, message, error, missing);
  }
  private String chatAnswer(String resultJson, String fallback) {
    if (resultJson == null) return fallback == null ? "暂时无法生成回答。" : fallback;
    String answer = tree(resultJson).path("answer").asText();
    return answer.isBlank() ? (fallback == null ? "暂时无法生成回答。" : fallback) : answer;
  }
  private String querySummary(String resultJson) {
    if (resultJson == null) return "未找到结果。";
    JsonNode result = tree(resultJson);
    if (result.has("conflictCount")) {
      int count = result.path("conflictCount").asInt();
      if (count == 0) return "本周没有发现同一天安排多项计划的冲突。";
      List<String> days = new ArrayList<>(); result.path("conflictDays").forEach(day -> days.add(day.asText()));
      return "发现 " + count + " 个安排冲突：" + String.join("、", days) + "。";
    }
    if (result.isArray()) return result.isEmpty() ? "未找到结果。" : "已找到 " + result.size() + " 条结果。";
    if (result.has("missingFields")) return "请补充必要信息后再查询。";
    return "查询已完成。";
  }
}
