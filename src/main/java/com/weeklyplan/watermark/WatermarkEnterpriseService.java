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

  @Transactional(readOnly = true)
  public List<WatermarkEnterpriseResponse> list() {
    access();
    return enterprises.findByCompanyIdOrderByCityAscCountyAscNameAsc(tenant.currentCompany().getId())
        .stream()
        .map(WatermarkEnterpriseResponse::of)
        .toList();
  }

  @Transactional
  public WatermarkEnterpriseImportResult importWorkbook(MultipartFile file) {
    access();
    Company company = tenant.currentCompany();
    List<ImportedWatermarkEnterprise> rows = workbook.parse(file);
    int inserted = 0, existed = 0, skipped = 0;
    for (ImportedWatermarkEnterprise row : rows) {
      String name = row.name().trim();
      String city = row.city().trim();
      String county = row.county().trim();
      if (name.isBlank() || county.isBlank()) {
        skipped += 1;
        continue;
      }
      if (enterprises.findByCompanyIdAndNameAndCityAndCounty(company.getId(), name, city, county).isPresent()) {
        existed += 1;
        continue;
      }
      enterprises.save(WatermarkEnterprise.create(company, name, city, county));
      inserted += 1;
    }
    return new WatermarkEnterpriseImportResult(inserted, existed, skipped);
  }

  private void access() {
    modules.requireEnabled(MODULE);
  }
}
