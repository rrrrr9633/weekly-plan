package com.weeklyplan.plan;

import com.weeklyplan.project.Project;
import com.weeklyplan.project.ProjectService;
import com.weeklyplan.project.ProjectStatus;
import com.weeklyplan.user.AppUser;
import com.weeklyplan.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WeekPlanService {
  private final WeekPlanRepository plans;
  private final ProjectService projects;
  private final UserRepository users;

  public WeekPlanService(WeekPlanRepository plans, ProjectService projects, UserRepository users) {
    this.plans = plans; this.projects = projects; this.users = users;
  }

  @Transactional(readOnly = true)
  public List<WeekPlanResponse> listMine(String userId, int year, int week) {
    return plans.findByUserIdAndYearAndWeekNumberAndStatusOrderByCreatedAtDesc(parseId(userId), year, week, PlanStatus.ACTIVE).stream()
        .sorted(this::compareByWeekdayThenCreatedAt).map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<WeekPlanResponse> listTeam(int year, int week) {
    return plans.findByYearAndWeekNumberOrderByUserDisplayNameAscCreatedAtDesc(year, week).stream()
        .sorted(this::compareByWeekdayThenBoardPositionThenCreatedAt).map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<WeekPlanResponse> listArchived(String userId) {
    return plans.findByUserIdAndStatusOrderByArchivedAtDesc(parseId(userId), PlanStatus.ARCHIVED).stream().map(this::toResponse).toList();
  }

  @Transactional
  public WeekPlanResponse create(String userId, CreateWeekPlanRequest request) {
    AppUser user = requireUser(parseId(userId));
    Project project = requireActiveProject(request.projectId());
    return toResponse(plans.save(WeekPlan.create(
        project, user, null, request.year(), request.weekNumber(), request.weekday(), request.content().trim())));
  }

  @Transactional
  public List<WeekPlanResponse> createBatch(String userId, BatchCreateWeekPlanRequest request) {
    AppUser user = requireUser(parseId(userId));
    Project project = requireActiveProject(request.projectId());
    List<WeekPlan> createdPlans = request.plans().stream()
        .map(item -> WeekPlan.create(
            project, user, null, request.year(), request.weekNumber(), item.weekday(), item.content().trim()))
        .toList();
    return plans.saveAll(createdPlans).stream().map(this::toResponse).toList();
  }

  @Transactional
  public WeekPlanResponse update(String userId, Long id, UpdateWeekPlanRequest request) {
    WeekPlan plan = plans.findByIdAndUserId(id, parseId(userId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在或无权修改"));
    if (plan.getAssignedBy() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员分配的计划不可由个人修改");
    plan.update(request.content().trim(), request.weekday());
    return toResponse(plan);
  }

  @Transactional
  public void delete(String userId, Long id) {
    WeekPlan plan = plans.findByIdAndUserId(id, parseId(userId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在或无权删除"));
    if (plan.getAssignedBy() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员分配的计划不可由个人删除");
    plans.delete(plan);
  }

  @Transactional
  public WeekPlanResponse archive(String userId, Long id) {
    WeekPlan plan = plans.findByIdAndUserId(id, parseId(userId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在或无权归档"));
    if (plan.getAssignedBy() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员分配的计划不可由个人归档");
    if (plan.getStatus() == PlanStatus.ARCHIVED) throw new ResponseStatusException(HttpStatus.CONFLICT, "计划已归档");
    plan.archive();
    return toResponse(plan);
  }

  @Transactional
  public WeekPlanResponse restore(String userId, Long id) {
    WeekPlan plan = plans.findByIdAndUserId(id, parseId(userId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "计划不存在或无权恢复"));
    if (plan.getAssignedBy() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员分配的计划不可由个人恢复");
    if (plan.getStatus() != PlanStatus.ARCHIVED) throw new ResponseStatusException(HttpStatus.CONFLICT, "计划未归档");
    plan.restore();
    return toResponse(plan);
  }

  @Transactional
  public List<WeekPlanResponse> saveBoardOrder(String userId, SaveBoardOrderRequest request) {
    requireUser(parseId(userId));
    List<WeekPlan> scopedPlans = plans.findByProjectIdAndYearAndWeekNumberAndWeekday(
        request.projectId(), request.year(), request.weekNumber(), request.weekday());
    Set<Long> submittedIds = new HashSet<>(request.planIds());
    Set<Long> scopedIds = scopedPlans.stream().map(WeekPlan::getId).collect(java.util.stream.Collectors.toSet());
    if (submittedIds.size() != request.planIds().size() || !submittedIds.equals(scopedIds)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排序计划必须完整且属于当前项目、周次和日期");
    }
    int position = 0;
    for (Long planId : request.planIds()) {
      WeekPlan plan = scopedPlans.stream().filter(item -> item.getId().equals(planId)).findFirst().orElseThrow();
      plan.setBoardPosition(position++);
    }
    return scopedPlans.stream().sorted(this::compareByWeekdayThenBoardPositionThenCreatedAt).map(this::toResponse).toList();
  }

  @Transactional
  public void restoreBoardOrder(String userId, Long projectId, int year, int weekNumber, PlanWeekday weekday) {
    requireUser(parseId(userId));
    plans.findByProjectIdAndYearAndWeekNumberAndWeekday(projectId, year, weekNumber, weekday)
        .forEach(plan -> plan.setBoardPosition(null));
  }

  @Transactional
  public WeekPlanResponse assign(String adminId, AssignWeekPlanRequest request) {
    AppUser admin = requireUser(parseId(adminId));
    Project project = requireActiveProject(request.projectId());
    AppUser user = requireUser(request.userId());
    return toResponse(plans.save(WeekPlan.create(
        project, user, admin, request.year(), request.weekNumber(), request.weekday(), request.content().trim())));
  }

  private WeekPlanResponse toResponse(WeekPlan plan) {
    LocalDate weekStart = LocalDate.of(plan.getYear(), 1, 4)
        .with(WeekFields.ISO.weekOfWeekBasedYear(), plan.getWeekNumber())
        .with(WeekFields.ISO.dayOfWeek(), 1);
    return new WeekPlanResponse(
        String.valueOf(plan.getId()), String.valueOf(plan.getProject().getId()), plan.getProject().getName(), plan.getProject().getCode(),
        String.valueOf(plan.getUser().getId()), plan.getUser().getDisplayName(), plan.getYear(), plan.getWeekNumber(),
        plan.getWeekday().name().toLowerCase(Locale.ROOT), weekStart, weekStart.plusDays(6), plan.getContent(),
        plan.getAssignedBy() == null ? null : plan.getAssignedBy().getDisplayName(), plan.getAssignedBy() != null,
        plan.getStatus().name().toLowerCase(Locale.ROOT), plan.getArchivedAt(),
        plan.getBoardPosition() == null ? null : plan.getBoardPosition().longValue(), plan.getCreatedAt(), plan.getUpdatedAt());
  }

  private int compareByWeekdayThenBoardPositionThenCreatedAt(WeekPlan left, WeekPlan right) {
    int weekdayOrder = Integer.compare(weekdayRank(left.getWeekday()), weekdayRank(right.getWeekday()));
    if (weekdayOrder != 0) return weekdayOrder;
    int leftPosition = left.getBoardPosition() == null ? Integer.MAX_VALUE : left.getBoardPosition();
    int rightPosition = right.getBoardPosition() == null ? Integer.MAX_VALUE : right.getBoardPosition();
    int positionOrder = Integer.compare(leftPosition, rightPosition);
    return positionOrder != 0 ? positionOrder : left.getCreatedAt().compareTo(right.getCreatedAt());
  }

  private int compareByWeekdayThenCreatedAt(WeekPlan left, WeekPlan right) {
    int weekdayOrder = Integer.compare(weekdayRank(left.getWeekday()), weekdayRank(right.getWeekday()));
    return weekdayOrder != 0 ? weekdayOrder : left.getCreatedAt().compareTo(right.getCreatedAt());
  }

  private int weekdayRank(PlanWeekday weekday) {
    return weekday == PlanWeekday.PENDING ? 0 : weekday.ordinal() + 1;
  }

  private Project requireActiveProject(Long id) {
    Project project = projects.requireProject(id);
    if (project.getStatus() != ProjectStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目已停用，不能创建计划");
    return project;
  }
  private AppUser requireUser(Long id) {
    return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
  }
  private Long parseId(String value) {
    try { return Long.valueOf(value); }
    catch (NumberFormatException error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效"); }
  }
}
