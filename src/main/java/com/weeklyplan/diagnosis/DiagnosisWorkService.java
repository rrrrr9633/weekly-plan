package com.weeklyplan.diagnosis;

import com.weeklyplan.company.Company;
import com.weeklyplan.module.FeatureModuleService;
import com.weeklyplan.tenant.TenantAccessService;
import com.weeklyplan.user.AppUser;
import com.weeklyplan.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class DiagnosisWorkService {
  private static final String MODULE = "TEAM_DIAGNOSIS_CALENDAR";
  private final DiagnosisWorkRepository works; private final UserRepository users; private final TenantAccessService tenant; private final FeatureModuleService modules;
  public DiagnosisWorkService(DiagnosisWorkRepository works, UserRepository users, TenantAccessService tenant, FeatureModuleService modules) { this.works = works; this.users = users; this.tenant = tenant; this.modules = modules; }
  @Transactional(readOnly = true) public List<DiagnosisWorkResponse> list(LocalDate start, LocalDate end, String enterprise, String participant) { modules.requireEnabled(MODULE); Company company = tenant.currentCompany(); List<String> names = participantNames(participant); List<DiagnosisWork> found = works.findByCompanyIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(company.getId(), start, end); found.forEach(work -> work.getParticipants().size()); if (enterprise != null && !enterprise.isBlank()) { String needle = compact(enterprise); found = found.stream().filter(work -> compact(work.getEnterpriseName()).contains(needle)).toList(); } if (!names.isEmpty()) found = found.stream().filter(work -> matchesAnyParticipant(work, participant, names)).toList(); return found.stream().map(this::response).toList(); }
  @Transactional public DiagnosisWorkResponse create(SaveDiagnosisWorkRequest request) {
    modules.requireEnabled(MODULE);
    Company company = tenant.currentCompany();
    String enterpriseName = request.enterpriseName().trim();
    DiagnosisWork existing = findSame(works.findByCompanyIdAndDiagnosisRound(company.getId(), request.diagnosisRound()), enterpriseName, request.diagnosisRound());
    return switch (ImportConflicts.decide(existing != null, request.keep()).action()) {
      case INSERT -> response(works.save(DiagnosisWork.create(company, tenant.currentUser(), request.workDate(), enterpriseName, request.county().trim(), request.diagnosisTime().trim(), request.diagnosisRound(), text(request.enterpriseContact()), text(request.enterpriseContactPhone()), members(company, request.participantIds()))));
      case KEEP_OLD -> response(existing);
      case REPLACE -> { existing.update(request.workDate(), enterpriseName, request.county().trim(), request.diagnosisTime().trim(), request.diagnosisRound(), text(request.enterpriseContact()), text(request.enterpriseContactPhone()), members(company, request.participantIds())); yield response(existing); }
      case NEED_CHOICE -> throw conflict(enterpriseName, request.diagnosisRound());
      case INVALID -> throw invalidKeep();
    };
  }
  @Transactional(readOnly = true) public List<ImportedDiagnosisRow> matchExistingEnterprises(List<ImportedDiagnosisRow> rows) {
    modules.requireEnabled(MODULE);
    List<DiagnosisWork> existing = loadWorks(tenant.currentCompany().getId());
    List<String> names = existing.stream().map(DiagnosisWork::getEnterpriseName).distinct().toList();
    return rows.stream().map(row -> annotate(row, names, existing)).toList();
  }
  @Transactional public DiagnosisWorkResponse update(Long id, SaveDiagnosisWorkRequest request) { modules.requireEnabled(MODULE); DiagnosisWork work = requireWork(id); work.update(request.workDate(), request.enterpriseName().trim(), request.county().trim(), request.diagnosisTime().trim(), request.diagnosisRound(), text(request.enterpriseContact()), text(request.enterpriseContactPhone()), members(work.getCompany(), request.participantIds())); return response(work); }
  @Transactional public void delete(Long id) { modules.requireEnabled(MODULE); works.delete(requireWork(id)); }
  private ImportedDiagnosisRow annotate(ImportedDiagnosisRow row, List<String> names, List<DiagnosisWork> existing) {
    EnterpriseNames.Match match = EnterpriseNames.best(row.enterpriseName(), names);
    DiagnosisWork same = findSame(existing, row.enterpriseName(), row.diagnosisRound());
    boolean sameRound = "相同".equals(match.nameMatch()) && same != null;
    return row.withMatch(match.nameMatch(), match.similarity(), match.matchedEnterpriseName(), sameRound, sameRound ? snapshot(same) : null);
  }
  @Transactional public List<DiagnosisImportItem> applyImport(List<ImportDiagnosisChoice> rows) {
    modules.requireEnabled(MODULE);
    Company company = tenant.currentCompany();
    List<DiagnosisWork> existing = new ArrayList<>(loadWorks(company.getId()));
    List<String> undecided = new ArrayList<>();
    List<Slot> occupied = new ArrayList<>(existing.stream().map(work -> new Slot(work.getEnterpriseName(), work.getDiagnosisRound())).toList());
    for (ImportDiagnosisChoice row : rows) {
      boolean taken = taken(occupied, row.enterpriseName(), row.diagnosisRound());
      ImportConflicts.Decision decision = ImportConflicts.decide(taken, row.keep());
      if (decision.action() == ImportConflicts.Action.INVALID) throw invalidKeep();
      if (decision.action() == ImportConflicts.Action.NEED_CHOICE) undecided.add(label(row.enterpriseName(), row.diagnosisRound()));
      if (decision.action() == ImportConflicts.Action.INSERT) occupied.add(new Slot(row.enterpriseName().trim(), row.diagnosisRound()));
    }
    if (!undecided.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "请先选择保留新的或旧的：" + String.join("、", undecided));
    List<AppUser> companyUsers = users.findByCompanyId(company.getId());
    List<DiagnosisImportItem> results = new ArrayList<>();
    for (ImportDiagnosisChoice row : rows) {
      String enterpriseName = row.enterpriseName().trim();
      DiagnosisWork hit = findSame(existing, enterpriseName, row.diagnosisRound());
      switch (ImportConflicts.decide(hit != null, row.keep()).action()) {
        case INSERT -> {
          DiagnosisWork created = works.save(DiagnosisWork.create(company, tenant.currentUser(), row.workDate(), enterpriseName, row.county().trim(), row.diagnosisTime().trim(), row.diagnosisRound(), text(row.enterpriseContact()), text(row.enterpriseContactPhone()), membersOf(company, companyUsers, row)));
          existing.add(created);
          results.add(new DiagnosisImportItem(enterpriseName, row.diagnosisRound(), "已新增", ""));
        }
        case KEEP_OLD -> results.add(new DiagnosisImportItem(enterpriseName, row.diagnosisRound(), "已保留旧的", "日历未改动"));
        case REPLACE -> {
          hit.update(row.workDate(), enterpriseName, row.county().trim(), row.diagnosisTime().trim(), row.diagnosisRound(), text(row.enterpriseContact()), text(row.enterpriseContactPhone()), membersOf(company, companyUsers, row));
          results.add(new DiagnosisImportItem(enterpriseName, row.diagnosisRound(), "已改为新的", "日期、区县、时间和参与人员已按本次表格覆盖"));
        }
        case NEED_CHOICE -> throw conflict(enterpriseName, row.diagnosisRound());
        case INVALID -> throw invalidKeep();
      }
    }
    return results;
  }
  private DiagnosisWork requireWork(Long id) { DiagnosisWork work = works.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "诊断工作不存在")); tenant.assertCompany(work.getCompany()); return work; }
  private Set<AppUser> members(Company company, List<Long> ids) { Set<Long> uniqueIds = new LinkedHashSet<>(ids); if (uniqueIds.size() != ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员不能重复"); List<AppUser> members = users.findAllById(uniqueIds); if (members.size() != uniqueIds.size() || members.stream().anyMatch(user -> user.getCompany() == null || !company.getId().equals(user.getCompany().getId()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员必须属于当前企业"); return new LinkedHashSet<>(members); }
  private DiagnosisWorkResponse response(DiagnosisWork work) { return new DiagnosisWorkResponse(String.valueOf(work.getId()), work.getWorkDate(), work.getEnterpriseName(), work.getAddress(), work.getCounty(), work.getDiagnosisTime(), work.getDiagnosisRound(), work.getEnterpriseContact(), work.getEnterpriseContactPhone(), displayName(work.getCreatedBy()), work.getParticipants().stream().map(user -> new DiagnosisWorkResponse.Participant(String.valueOf(user.getId()), displayName(user))).toList(), work.getCreatedAt(), work.getUpdatedAt()); }
  private String text(String value) { return value == null ? "" : value.trim(); }
  private List<String> participantNames(String value) { if (value == null || value.isBlank()) return List.of(); List<String> names = java.util.Arrays.stream(value.split("[，,、。.；;／/|]+")).map(String::trim).filter(item -> !item.isBlank()).distinct().toList(); if (names.size() > 3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "诊断人员最多同时搜索三人"); return names; }
  private boolean matchesAnyParticipant(DiagnosisWork work, String raw, List<String> names) { String query = compact(raw); return work.getParticipants().stream().anyMatch(user -> { String display = compact(displayName(user)); String username = compact(user.getUsername()); return containsName(query, names, display) || containsName(query, names, username); }); }
  private boolean containsName(String query, List<String> names, String value) { if (value.length() < 2) return false; if (query.contains(value)) return true; return names.stream().map(this::compact).anyMatch(name -> !name.isBlank() && (value.contains(name) || name.contains(value))); }
  private String compact(String value) { return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT); }
  private String displayName(AppUser user) { return user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName(); }
  private List<DiagnosisWork> loadWorks(Long companyId) { List<DiagnosisWork> existing = works.findByCompanyId(companyId); existing.forEach(work -> work.getParticipants().size()); return existing; }
  private DiagnosisWork findSame(List<DiagnosisWork> existing, String enterpriseName, Integer round) { return existing.stream().filter(work -> Objects.equals(round, work.getDiagnosisRound()) && EnterpriseNames.same(work.getEnterpriseName(), enterpriseName)).findFirst().orElse(null); }
  private boolean taken(List<Slot> occupied, String enterpriseName, Integer round) { return occupied.stream().anyMatch(slot -> Objects.equals(round, slot.round()) && EnterpriseNames.same(slot.enterpriseName(), enterpriseName)); }
  private ImportedDiagnosisRow.Existing snapshot(DiagnosisWork work) { return new ImportedDiagnosisRow.Existing(String.valueOf(work.getId()), work.getWorkDate(), work.getEnterpriseName(), work.getCounty(), work.getDiagnosisTime(), work.getDiagnosisRound(), work.getEnterpriseContact(), work.getEnterpriseContactPhone(), work.getParticipants().stream().map(this::displayName).toList()); }
  private Set<AppUser> membersOf(Company company, List<AppUser> companyUsers, ImportDiagnosisChoice row) {
    if (row.participantIds() != null && !row.participantIds().isEmpty()) return members(company, row.participantIds());
    List<String> names = row.participantNames() == null ? List.of() : row.participantNames();
    List<String> missing = new ArrayList<>();
    Set<AppUser> found = new LinkedHashSet<>();
    for (String name : names) {
      if (name == null || name.isBlank()) continue;
      AppUser user = matchUser(companyUsers, name.trim());
      if (user == null) missing.add(name.trim());
      else found.add(user);
    }
    if (!missing.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员无法对应到当前企业：" + String.join("、", missing));
    if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员不能为空");
    return found;
  }
  private AppUser matchUser(List<AppUser> companyUsers, String name) {
    String needle = compact(name);
    List<AppUser> hits = companyUsers.stream().filter(user -> needle.equals(compact(displayName(user))) || needle.equals(compact(user.getUsername()))).toList();
    return hits.stream().filter(user -> needle.equals(compact(displayName(user)))).findFirst().orElse(hits.isEmpty() ? null : hits.get(0));
  }
  private ResponseStatusException conflict(String enterpriseName, Integer round) { return new ResponseStatusException(HttpStatus.CONFLICT, label(enterpriseName, round) + "已有记录，请选择保留新的或旧的"); }
  private ResponseStatusException invalidKeep() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "保留选择只能是新的或旧的"); }
  private String label(String enterpriseName, Integer round) { return enterpriseName.trim() + "第" + round + "次"; }
  private record Slot(String enterpriseName, Integer round) {}
}
