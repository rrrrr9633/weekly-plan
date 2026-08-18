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
    AiDtos.ModelProposal model = client.propose(message, candidates, today.get(iso.weekBasedYear()), today.get(iso.weekOfWeekBasedYear()));
    AiOperationType type = parseType(model.operationType()); JsonNode payload = model.payload();
    if (payload == null || !payload.isObject() || containsForbiddenField(payload)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 提案包含不允许的身份或租户字段");
    ObjectNode completed = ((ObjectNode) payload).deepCopy(); Object missing = completePayload(type, completed, user);
    AiOperationProposal proposal = proposals.save(AiOperationProposal.create(tenant.currentCompany(), user, type, json(completed), preview(type, completed, model.preview())));
    if (type == AiOperationType.QUERY) proposal.completeReadOnly(json(missing == null ? query(completed) : Map.of("missingFields", missing, "readOnly", true)));
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
  private Object completePayload(AiOperationType type, ObjectNode p, AppUser user) {
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
  private Object targetHints(AiOperationType type, AppUser user) { if (type != AiOperationType.PLAN_UPDATE && type != AiOperationType.PLAN_DELETE) return List.of(); return planRepository.findAll().stream().filter(x -> x.getProject().getCompany().getId().equals(tenant.currentCompany().getId()) && x.getUser().getId().equals(user.getId())).map(x -> Map.of("id",x.getId(),"content",x.getContent(),"project",x.getProject().getName(),"weekday",x.getWeekday().name())).toList(); }
  private String preview(AiOperationType type, JsonNode p, String modelPreview) { return modelPreview == null || modelPreview.isBlank() ? type.name() + ": " + p.toString() : modelPreview; }
  private void fill(ObjectNode p, String field, Object value) { if (!p.hasNonNull(field) && value != null) p.set(field, mapper.valueToTree(value)); }
  private void required(JsonNode p, List<String> fields, String field) { if (!p.hasNonNull(field) || p.path(field).asText().isBlank()) fields.add(field); }
  private boolean containsForbiddenField(JsonNode node) { if (node.isObject()) { var it=node.fieldNames(); while(it.hasNext()) { String k=it.next().toLowerCase(); if(k.contains("user")||k.contains("company")||k.contains("role")||k.contains("auth")||k.contains("member")) return true; } } for(JsonNode c:node) if(containsForbiddenField(c)) return true; return false; }
  private AiOperationType parseType(String v) { try{return AiOperationType.valueOf(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"AI 返回了不支持的操作类型");} }
  private Long requiredId(JsonNode p) { if(!p.hasNonNull("id")||p.path("id").asLong()<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"缺少资源 id"); return p.path("id").asLong(); }
  private PlanWeekday weekday(JsonNode p) { try{return PlanWeekday.valueOf(p.path("weekday").asText().toUpperCase());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"weekday 无效");} }
  private String nullable(JsonNode p,String f){return p.hasNonNull(f)?p.path(f).asText():null;} private String json(Object v){try{return mapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException(e);}} private JsonNode tree(String v){try{return mapper.readTree(v);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"AI 提案数据无效");}}
  private AiDtos.ProposalResponse response(AiOperationProposal p,Object missing){return new AiDtos.ProposalResponse(p.getId(),p.getOperationType().name(),p.getStatus().name(),tree(p.getPayload()),p.getPreview(),p.getResultJson()==null?null:tree(p.getResultJson()),p.getErrorMessage(),missing);}
}
