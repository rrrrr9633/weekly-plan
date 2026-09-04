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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DiagnosisWorkService {
  private static final String MODULE = "TEAM_DIAGNOSIS_CALENDAR";
  private final DiagnosisWorkRepository works; private final UserRepository users; private final TenantAccessService tenant; private final FeatureModuleService modules;
  public DiagnosisWorkService(DiagnosisWorkRepository works, UserRepository users, TenantAccessService tenant, FeatureModuleService modules) { this.works = works; this.users = users; this.tenant = tenant; this.modules = modules; }
  @Transactional(readOnly = true) public List<DiagnosisWorkResponse> list(LocalDate start, LocalDate end) { modules.requireEnabled(MODULE); Company company = tenant.currentCompany(); return works.findByCompanyIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(company.getId(), start, end).stream().map(this::response).toList(); }
  @Transactional public DiagnosisWorkResponse create(SaveDiagnosisWorkRequest request) { modules.requireEnabled(MODULE); Company company = tenant.currentCompany(); String enterpriseName = request.enterpriseName().trim(); DiagnosisWork existing = works.findByCompanyIdAndDiagnosisRound(company.getId(), request.diagnosisRound()).stream().filter(work -> work.getEnterpriseName().trim().equalsIgnoreCase(enterpriseName)).findFirst().orElse(null); Set<AppUser> incomingMembers = members(company, request.participantIds()); if (existing != null) { existing.mergeParticipants(incomingMembers); return response(existing); } return response(works.save(DiagnosisWork.create(company, tenant.currentUser(), request.workDate(), enterpriseName, request.county().trim(), request.diagnosisTime().trim(), request.diagnosisRound(), text(request.enterpriseContact()), text(request.enterpriseContactPhone()), incomingMembers))); }
  @Transactional public DiagnosisWorkResponse update(Long id, SaveDiagnosisWorkRequest request) { modules.requireEnabled(MODULE); DiagnosisWork work = requireWork(id); work.update(request.workDate(), request.enterpriseName().trim(), request.county().trim(), request.diagnosisTime().trim(), request.diagnosisRound(), text(request.enterpriseContact()), text(request.enterpriseContactPhone()), members(work.getCompany(), request.participantIds())); return response(work); }
  @Transactional public void delete(Long id) { modules.requireEnabled(MODULE); works.delete(requireWork(id)); }
  private DiagnosisWork requireWork(Long id) { DiagnosisWork work = works.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "诊断工作不存在")); tenant.assertCompany(work.getCompany()); return work; }
  private Set<AppUser> members(Company company, List<Long> ids) { Set<Long> uniqueIds = new LinkedHashSet<>(ids); if (uniqueIds.size() != ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员不能重复"); List<AppUser> members = users.findAllById(uniqueIds); if (members.size() != uniqueIds.size() || members.stream().anyMatch(user -> user.getCompany() == null || !company.getId().equals(user.getCompany().getId()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参与人员必须属于当前企业"); return new LinkedHashSet<>(members); }
  private DiagnosisWorkResponse response(DiagnosisWork work) { return new DiagnosisWorkResponse(String.valueOf(work.getId()), work.getWorkDate(), work.getEnterpriseName(), work.getAddress(), work.getCounty(), work.getDiagnosisTime(), work.getDiagnosisRound(), work.getEnterpriseContact(), work.getEnterpriseContactPhone(), displayName(work.getCreatedBy()), work.getParticipants().stream().map(user -> new DiagnosisWorkResponse.Participant(String.valueOf(user.getId()), displayName(user))).toList(), work.getCreatedAt(), work.getUpdatedAt()); }
  private String text(String value) { return value == null ? "" : value.trim(); }
  private String displayName(AppUser user) { return user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName(); }
}
