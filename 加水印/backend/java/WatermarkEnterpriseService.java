package com.weeklyplan.watermark;

import com.weeklyplan.company.Company;
import com.weeklyplan.module.FeatureModuleService;
import com.weeklyplan.tenant.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class WatermarkEnterpriseService {
  private static final String MODULE = "PHOTO_WATERMARK";
  private final WatermarkEnterpriseRepository enterprises;
  private final WatermarkEnterpriseWorkbookService workbook;
  private final TenantAccessService tenant;
  private final FeatureModuleService modules;

  public WatermarkEnterpriseService(
      WatermarkEnterpriseRepository enterprises,
      WatermarkEnterpriseWorkbookService workbook,
      TenantAccessService tenant,
      FeatureModuleService modules
  ) {
    this.enterprises = enterprises;
    this.workbook = workbook;
    this.tenant = tenant;
    this.modules = modules;
  }

  @Transactional
  public List<WatermarkEnterpriseResponse> list() {
    access();
    Long companyId = tenant.currentCompany().getId();
    collapseDuplicates(companyId);
    return enterprises.findByCompanyIdOrderByCityAscCountyAscNameAsc(companyId)
        .stream()
        .map(WatermarkEnterpriseResponse::of)
        .toList();
  }

  @Transactional
  public WatermarkEnterpriseImportResult importWorkbook(MultipartFile file) {
    access();
    Company company = tenant.currentCompany();
    collapseDuplicates(company.getId());
    List<WatermarkEnterprise> currentRows =
        enterprises.findByCompanyIdOrderByCityAscCountyAscNameAsc(company.getId());
    List<ImportedWatermarkEnterprise> rows = workbook.parse(file);
    int inserted = 0, updated = 0, unchanged = 0, skipped = 0;
    for (ImportedWatermarkEnterprise row : rows) {
      String name = WatermarkPlaceText.clean(row.name());
      String city = WatermarkPlaceText.clean(row.city());
      String county = WatermarkPlaceText.clean(row.county());
      if (name.isBlank() || county.isBlank()) {
        skipped += 1;
        continue;
      }
      WatermarkEnterprise existing = WatermarkEnterpriseDeduper.match(currentRows, name, city, county);
      if (existing == null) {
        WatermarkEnterprise created = enterprises.save(WatermarkEnterprise.create(
            company, name, city, county, row.visit1At(), row.visit2At(), row.visit3At()
        ));
        currentRows.add(created);
        inserted += 1;
        continue;
      }
      existing.completePlace(city, county);
      if (!row.visitsProvided() || existing.sameVisits(row.visit1At(), row.visit2At(), row.visit3At())) {
        unchanged += 1;
        continue;
      }
      existing.replaceVisits(row.visit1At(), row.visit2At(), row.visit3At());
      updated += 1;
    }
    return new WatermarkEnterpriseImportResult(inserted, updated, unchanged, skipped);
  }

  private void collapseDuplicates(Long companyId) {
    WatermarkEnterpriseDeduper.Plan plan = WatermarkEnterpriseDeduper.plan(
        enterprises.findByCompanyIdOrderByCityAscCountyAscNameAsc(companyId)
    );
    if (plan.drop().isEmpty()) return;
    enterprises.deleteAll(plan.drop());
    enterprises.flush();
    WatermarkEnterpriseDeduper.mergeIntoKeepers(plan);
  }

  private void access() {
    modules.requireEnabled(MODULE);
  }
}
