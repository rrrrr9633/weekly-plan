package com.weeklyplan.watermark;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface WatermarkEnterpriseRepository extends JpaRepository<WatermarkEnterprise, Long> {
  List<WatermarkEnterprise> findByCompanyIdOrderByCityAscCountyAscNameAsc(Long companyId);
}
